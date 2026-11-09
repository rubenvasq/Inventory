package co.domio.stockcontrol.project;

import co.domio.stockcontrol.common.dto.PageResponse;
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
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Logica de negocio para proyectos e instalaciones de clientes.
 *
 * <p>Las asignaciones planifican material para un proyecto; las entregas
 * descuentan existencias y las devoluciones las reintegran. Toda entrega o
 * devolucion se ejecuta en una sola transaccion junto con el movimiento de
 * inventario vinculado al proyecto.</p>
 */
@Service
@Transactional
public class ProjectService {

    private static final String RESOURCE = "Proyecto";

    private final ProjectRepository projectRepository;
    private final ProjectAssignmentRepository assignmentRepository;
    private final ProductRepository productRepository;
    private final StockMovementRepository movementRepository;

    public ProjectService(ProjectRepository projectRepository, ProjectAssignmentRepository assignmentRepository,
            ProductRepository productRepository, StockMovementRepository movementRepository) {
        this.projectRepository = projectRepository;
        this.assignmentRepository = assignmentRepository;
        this.productRepository = productRepository;
        this.movementRepository = movementRepository;
    }

    /** Busca proyectos con paginacion del lado del servidor y texto contra nombre o cliente. */
    @Transactional(readOnly = true)
    public PageResponse<ProjectResponse> search(String search, ProjectStatus status, Pageable pageable) {
        String pattern = (search == null || search.isBlank())
                ? null
                : "%" + search.trim().toLowerCase() + "%";

        Specification<Project> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (pattern != null) {
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(root.get("client")), pattern)));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Project> page = projectRepository.findAll(spec, pageable);
        Map<Long, List<AssignmentResponse>> assignmentsByProject = assignmentsByProject(page.getContent());
        return PageResponse.from(page.map(p -> ProjectResponse.from(p,
                assignmentsByProject.getOrDefault(p.getId(), List.of()))));
    }

    @Transactional(readOnly = true)
    public ProjectResponse findById(Long id) {
        Project project = findProject(id);
        return ProjectResponse.from(project,
                assignmentRepository.findByProjectIdOrderByIdAsc(id).stream()
                        .map(AssignmentResponse::from).toList());
    }

    public ProjectResponse create(ProjectRequest request) {
        Project project = new Project();
        apply(project, request);
        return ProjectResponse.from(projectRepository.save(project), List.of());
    }

    public ProjectResponse update(Long id, ProjectRequest request) {
        Project project = findProject(id);
        apply(project, request);
        project = projectRepository.save(project);
        return ProjectResponse.from(project,
                assignmentRepository.findByProjectIdOrderByIdAsc(id).stream()
                        .map(AssignmentResponse::from).toList());
    }

    /**
     * Elimina un proyecto. Si ya tiene asignaciones o movimientos vinculados,
     * el historial debe conservarse: se rechaza el borrado con un error de
     * negocio (el proyecto puede cancelarse cambiando su estado).
     */
    public void delete(Long id) {
        Project project = findProject(id);
        if (assignmentRepository.existsByProjectId(id) || movementRepository.existsByProjectId(id)) {
            throw new BusinessRuleException(
                    "El proyecto '%s' tiene material asignado o movimientos vinculados y no puede eliminarse; "
                            + "cancelalo cambiando su estado para conservar el historial".formatted(project.getName()));
        }
        projectRepository.delete(project);
    }

    /**
     * Asigna material a un proyecto. Si el producto ya estaba asignado, la
     * cantidad se suma a la asignacion existente (no se descuenta stock).
     */
    public AssignmentResponse assign(Long projectId, AssignmentRequest request) {
        Project project = findProject(projectId);
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> ResourceNotFoundException.of("Producto", request.productId()));
        requirePositive(request.quantity());

        ProjectAssignment assignment = assignmentRepository
                .findByProjectIdAndProductId(projectId, request.productId())
                .orElseGet(() -> {
                    ProjectAssignment created = new ProjectAssignment();
                    created.setProject(project);
                    created.setProduct(product);
                    created.setAssignedQuantity(0);
                    created.setDeliveredQuantity(0);
                    created.setReturnedQuantity(0);
                    return created;
                });
        assignment.setAssignedQuantity(assignment.getAssignedQuantity() + request.quantity());
        return AssignmentResponse.from(assignmentRepository.save(assignment));
    }

    /** Retira una asignacion; solo posible si el material no se ha entregado al proyecto. */
    public void removeAssignment(Long projectId, Long assignmentId) {
        findProject(projectId);
        ProjectAssignment assignment = assignmentRepository.findById(assignmentId)
                .filter(a -> a.getProject().getId().equals(projectId))
                .orElseThrow(() -> ResourceNotFoundException.of("Asignacion", assignmentId));
        if (assignment.getDeliveredQuantity() > 0) {
            throw new BusinessRuleException(
                    "La asignacion ya tiene %d unidades entregadas y no puede retirarse".formatted(
                            assignment.getDeliveredQuantity()));
        }
        assignmentRepository.delete(assignment);
    }

    /**
     * Entrega material al proyecto: descuenta existencias, registra el
     * movimiento de salida vinculado al proyecto y avanza el acumulado
     * entregado, todo en una misma transaccion y con bloqueo del producto.
     */
    public MovementResponse deliver(Long projectId, ProjectMaterialRequest request) {
        Project project = findProject(projectId);
        if (project.getStatus() != ProjectStatus.PLANNED && project.getStatus() != ProjectStatus.IN_PROGRESS) {
            throw new BusinessRuleException(
                    "El proyecto esta en estado %s y no admite entregas de material".formatted(project.getStatus()));
        }
        return moveMaterial(project, request, MovementType.OUT, "Entrega al proyecto '%s'");
    }

    /**
     * Devuelve material del proyecto al inventario: reintegra existencias y
     * registra el movimiento de entrada vinculado al proyecto. Permitida en
     * cualquier estado del proyecto (al cancelar o cerrar puede sobrar material).
     */
    public MovementResponse returnMaterial(Long projectId, ProjectMaterialRequest request) {
        Project project = findProject(projectId);
        return moveMaterial(project, request, MovementType.IN, "Devolucion del proyecto '%s'");
    }

    @Transactional(readOnly = true)
    public List<MovementResponse> findMovements(Long projectId) {
        findProject(projectId);
        return movementRepository.findByProjectIdOrderByIdDesc(projectId).stream()
                .map(m -> MovementResponse.from(m, null))
                .toList();
    }

    private MovementResponse moveMaterial(Project project, ProjectMaterialRequest request, MovementType type,
            String noteTemplate) {
        int quantity = requirePositive(request.quantity());

        // Bloqueo de escritura: serializa entregas y devoluciones del producto.
        Product product = productRepository.findByIdForUpdate(request.productId())
                .orElseThrow(() -> ResourceNotFoundException.of("Producto", request.productId()));

        ProjectAssignment assignment = assignmentRepository
                .findByProjectIdAndProductId(project.getId(), request.productId())
                .orElseThrow(() -> new BusinessRuleException(
                        "El producto '%s' no esta asignado al proyecto".formatted(product.getName())));

        int previousStock = product.getStock();
        int newStock;
        if (type == MovementType.OUT) {
            if (assignment.getDeliveredQuantity() + quantity > assignment.getAssignedQuantity()) {
                throw new BusinessRuleException(
                        "La entrega supera la cantidad asignada: asignadas %d, entregadas %d, entrega solicitada %d"
                                .formatted(assignment.getAssignedQuantity(), assignment.getDeliveredQuantity(),
                                        quantity));
            }
            if (quantity > previousStock) {
                throw new BusinessRuleException(
                        "Stock insuficiente: existencias actuales %d, entrega solicitada %d"
                                .formatted(previousStock, quantity));
            }
            newStock = previousStock - quantity;
            assignment.setDeliveredQuantity(assignment.getDeliveredQuantity() + quantity);
        } else {
            if (assignment.getReturnedQuantity() + quantity > assignment.getDeliveredQuantity()) {
                throw new BusinessRuleException(
                        "La devolucion supera las unidades entregadas: entregadas %d, devueltas %d, devolucion solicitada %d"
                                .formatted(assignment.getDeliveredQuantity(), assignment.getReturnedQuantity(),
                                        quantity));
            }
            newStock = previousStock + quantity;
            assignment.setReturnedQuantity(assignment.getReturnedQuantity() + quantity);
        }

        product.setStock(newStock);
        productRepository.save(product);
        assignmentRepository.save(assignment);

        StockMovement movement = new StockMovement();
        movement.setProduct(product);
        movement.setMovementType(type);
        movement.setQuantity(quantity);
        movement.setPreviousStock(previousStock);
        movement.setNewStock(newStock);
        movement.setNote(noteOrTemplate(request.note(), noteTemplate, project));
        movement.setProjectId(project.getId());
        movementRepository.save(movement);

        return MovementResponse.from(movement, project.getName());
    }

    private String noteOrTemplate(String note, String template, Project project) {
        return (note == null || note.isBlank()) ? template.formatted(project.getName()) : note.trim();
    }

    private void apply(Project project, ProjectRequest request) {
        project.setName(request.name().trim());
        project.setClient(request.client().trim());
        if (request.status() != null) {
            project.setStatus(request.status());
        }
        project.setStartDate(request.startDate());
        project.setEndDate(request.endDate());
        project.setNotes(request.notes());
    }

    private Project findProject(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of(RESOURCE, id));
    }

    private int requirePositive(int quantity) {
        if (quantity <= 0) {
            throw new BusinessRuleException("La cantidad debe ser mayor que cero");
        }
        return quantity;
    }

    /** Asignaciones de varios proyectos en una sola consulta (evita N+1 en el listado). */
    private Map<Long, List<AssignmentResponse>> assignmentsByProject(List<Project> projects) {
        List<Long> projectIds = projects.stream().map(Project::getId).toList();
        if (projectIds.isEmpty()) {
            return Map.of();
        }
        // El id del proyecto se lee del proxy sin inicializarlo: los proyectos
        // de la pagina ya estan en el contexto de persistencia.
        Map<Long, List<AssignmentResponse>> byProject = new java.util.HashMap<>();
        for (ProjectAssignment assignment : assignmentRepository.findByProjectIdInOrderByIdAsc(projectIds)) {
            byProject.computeIfAbsent(assignment.getProject().getId(), key -> new ArrayList<>())
                    .add(AssignmentResponse.from(assignment));
        }
        return byProject;
    }
}
