package co.domio.stockcontrol.project;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.domio.stockcontrol.IntegrationTest;
import co.domio.stockcontrol.category.Category;
import co.domio.stockcontrol.category.CategoryRepository;
import co.domio.stockcontrol.common.exception.BusinessRuleException;
import co.domio.stockcontrol.common.exception.ResourceNotFoundException;
import co.domio.stockcontrol.movement.MovementType;
import co.domio.stockcontrol.movement.StockMovement;
import co.domio.stockcontrol.movement.StockMovementRepository;
import co.domio.stockcontrol.movement.dto.MovementResponse;
import co.domio.stockcontrol.product.Product;
import co.domio.stockcontrol.product.ProductRepository;
import co.domio.stockcontrol.project.dto.AssignmentRequest;
import co.domio.stockcontrol.project.dto.AssignmentResponse;
import co.domio.stockcontrol.project.dto.ProjectMaterialRequest;
import co.domio.stockcontrol.project.dto.ProjectRequest;
import co.domio.stockcontrol.project.dto.ProjectResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class ProjectServiceIT extends IntegrationTest {

    @Autowired
    ProjectService service;
    @Autowired
    ProjectRepository projectRepository;
    @Autowired
    ProjectAssignmentRepository assignmentRepository;
    @Autowired
    ProductRepository productRepository;
    @Autowired
    CategoryRepository categoryRepository;
    @Autowired
    StockMovementRepository movementRepository;

    private Product newProduct(int stock) {
        Category category = new Category();
        category.setName("Cat-" + System.nanoTime());
        categoryRepository.save(category);
        Product product = new Product();
        product.setSku("PRJ-" + System.nanoTime());
        product.setName("Producto de proyecto");
        product.setCategory(category);
        product.setStock(stock);
        product.setMinStock(0);
        return productRepository.save(product);
    }

    private ProjectResponse newProject() {
        ProjectRequest request = new ProjectRequest("Instalacion " + System.nanoTime(), "Cliente prueba",
                ProjectStatus.IN_PROGRESS, null, null, null);
        return service.create(request);
    }

    @Test
    void asignarMaterial_acumulaSiElProductoYaEstaAsignado() {
        ProjectResponse project = newProject();
        Product product = newProduct(50);

        AssignmentResponse first = service.assign(project.id(), new AssignmentRequest(product.getId(), 5));
        AssignmentResponse second = service.assign(project.id(), new AssignmentRequest(product.getId(), 3));

        assertThat(first.assignedQuantity()).isEqualTo(5);
        assertThat(second.assignedQuantity()).isEqualTo(8);
        assertThat(second.deliveredQuantity()).isZero();
        // La asignacion no descuenta stock.
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStock()).isEqualTo(50);
    }

    @Test
    void entregarMaterial_descuentaStock_registraMovimientoVinculadoYAvanzaAcumulados() {
        ProjectResponse project = newProject();
        Product product = newProduct(50);
        service.assign(project.id(), new AssignmentRequest(product.getId(), 10));

        MovementResponse movement = service.deliver(project.id(),
                new ProjectMaterialRequest(product.getId(), 4, "entrega en camioneta"));

        assertThat(movement.type()).isEqualTo(MovementType.OUT);
        assertThat(movement.previousStock()).isEqualTo(50);
        assertThat(movement.newStock()).isEqualTo(46);
        assertThat(movement.projectId()).isEqualTo(project.id());
        assertThat(movement.projectName()).isEqualTo(project.name());
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStock()).isEqualTo(46);

        AssignmentResponse assignment = service.findById(project.id()).assignments().get(0);
        assertThat(assignment.deliveredQuantity()).isEqualTo(4);
        assertThat(assignment.pendingQuantity()).isEqualTo(6);
    }

    @Test
    void entregarMasQueLoAsignado_lanzaBusinessRule_yNoCambiaNada() {
        ProjectResponse project = newProject();
        Product product = newProduct(50);
        service.assign(project.id(), new AssignmentRequest(product.getId(), 3));

        assertThatThrownBy(() -> service.deliver(project.id(),
                new ProjectMaterialRequest(product.getId(), 4, null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("asignada");

        assertThat(productRepository.findById(product.getId()).orElseThrow().getStock()).isEqualTo(50);
        assertThat(movementRepository.findByProjectIdOrderByIdDesc(project.id())).isEmpty();
        assertThat(service.findById(project.id()).assignments().get(0).deliveredQuantity()).isZero();
    }

    @Test
    void entregarMasDelStockDisponible_lanzaBusinessRule() {
        ProjectResponse project = newProject();
        Product product = newProduct(5);
        service.assign(project.id(), new AssignmentRequest(product.getId(), 50));

        assertThatThrownBy(() -> service.deliver(project.id(),
                new ProjectMaterialRequest(product.getId(), 6, null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Stock insuficiente");

        assertThat(productRepository.findById(product.getId()).orElseThrow().getStock()).isEqualTo(5);
    }

    @Test
    void entregarProductoNoAsignado_lanzaBusinessRule() {
        ProjectResponse project = newProject();
        Product product = newProduct(5);

        assertThatThrownBy(() -> service.deliver(project.id(),
                new ProjectMaterialRequest(product.getId(), 1, null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("no esta asignado");
    }

    @Test
    void devolverMaterial_reintegraStockYRegistraEntradaVinculada() {
        ProjectResponse project = newProject();
        Product product = newProduct(50);
        service.assign(project.id(), new AssignmentRequest(product.getId(), 10));
        service.deliver(project.id(), new ProjectMaterialRequest(product.getId(), 4, null));

        MovementResponse movement = service.returnMaterial(project.id(),
                new ProjectMaterialRequest(product.getId(), 3, "sobrante en obra"));

        assertThat(movement.type()).isEqualTo(MovementType.IN);
        assertThat(movement.newStock()).isEqualTo(49); // 50 - 4 + 3
        assertThat(movement.projectId()).isEqualTo(project.id());
        assertThat(productRepository.findById(product.getId()).orElseThrow().getStock()).isEqualTo(49);

        AssignmentResponse assignment = service.findById(project.id()).assignments().get(0);
        assertThat(assignment.returnedQuantity()).isEqualTo(3);
        assertThat(assignment.deliveredQuantity()).isEqualTo(4);
    }

    @Test
    void devolverMasDeLoEntregado_lanzaBusinessRule_yNoCambiaNada() {
        ProjectResponse project = newProject();
        Product product = newProduct(50);
        service.assign(project.id(), new AssignmentRequest(product.getId(), 10));
        service.deliver(project.id(), new ProjectMaterialRequest(product.getId(), 2, null));

        assertThatThrownBy(() -> service.returnMaterial(project.id(),
                new ProjectMaterialRequest(product.getId(), 3, null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("devolucion supera");

        assertThat(productRepository.findById(product.getId()).orElseThrow().getStock()).isEqualTo(48);
    }

    @Test
    void proyectoCerrado_noAdmiteEntregas_peroSiDevoluciones() {
        ProjectResponse project = newProject();
        Product product = newProduct(50);
        service.assign(project.id(), new AssignmentRequest(product.getId(), 10));
        service.deliver(project.id(), new ProjectMaterialRequest(product.getId(), 5, null));

        ProjectResponse closed = service.update(project.id(), new ProjectRequest(project.name(),
                project.client(), ProjectStatus.COMPLETED, null, null, null));

        assertThatThrownBy(() -> service.deliver(closed.id(),
                new ProjectMaterialRequest(product.getId(), 1, null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("no admite entregas");

        // La devolucion sigue posible (material sobrante al cerrar).
        MovementResponse returned = service.returnMaterial(closed.id(),
                new ProjectMaterialRequest(product.getId(), 5, "material no usado"));
        assertThat(returned.type()).isEqualTo(MovementType.IN);
    }

    @Test
    void cancelarProyecto_conservaHistorialDeMovimientos() {
        ProjectResponse project = newProject();
        Product product = newProduct(50);
        service.assign(project.id(), new AssignmentRequest(product.getId(), 10));
        service.deliver(project.id(), new ProjectMaterialRequest(product.getId(), 7, null));

        service.update(project.id(), new ProjectRequest(project.name(), project.client(),
                ProjectStatus.CANCELLED, null, null, null));

        List<MovementResponse> history = service.findMovements(project.id());
        assertThat(history).hasSize(1);
        assertThat(history.get(0).type()).isEqualTo(MovementType.OUT);
        assertThat(history.get(0).quantity()).isEqualTo(7);
        assertThat(service.findById(project.id()).status()).isEqualTo(ProjectStatus.CANCELLED);
    }

    @Test
    void eliminarProyecto_conActividad_lanzaBusinessRule_yConservaHistorial() {
        ProjectResponse project = newProject();
        Product product = newProduct(50);
        service.assign(project.id(), new AssignmentRequest(product.getId(), 10));

        assertThatThrownBy(() -> service.delete(project.id()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("no puede eliminarse");

        // Un proyecto sin asignaciones ni movimientos si se puede borrar.
        ProjectResponse empty = newProject();
        service.delete(empty.id());
        assertThatThrownBy(() -> service.findById(empty.id()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void retirarAsignacion_conEntregas_lanzaBusinessRule() {
        ProjectResponse project = newProject();
        Product product = newProduct(50);
        AssignmentResponse assignment = service.assign(project.id(),
                new AssignmentRequest(product.getId(), 10));
        service.deliver(project.id(), new ProjectMaterialRequest(product.getId(), 2, null));

        assertThatThrownBy(() -> service.removeAssignment(project.id(), assignment.id()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("entregadas");

        // Sin entregas, la asignacion si puede retirarse.
        Product otherProduct = newProduct(30);
        AssignmentResponse pending = service.assign(project.id(),
                new AssignmentRequest(otherProduct.getId(), 3));
        service.removeAssignment(project.id(), pending.id());
    }

    @Test
    void historialDelProductoIncluyeMovimientosDelProyecto() {
        ProjectResponse project = newProject();
        Product product = newProduct(50);
        service.assign(project.id(), new AssignmentRequest(product.getId(), 10));
        service.deliver(project.id(), new ProjectMaterialRequest(product.getId(), 4, null));

        List<StockMovement> productHistory = movementRepository.findByProductIdOrderByIdDesc(product.getId());
        assertThat(productHistory).hasSize(1);
        assertThat(productHistory.get(0).getProjectId()).isEqualTo(project.id());
        assertThat(productHistory.get(0).getNewStock()).isEqualTo(46);
    }
}
