package co.domio.stockcontrol.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.domio.stockcontrol.IntegrationTest;
import co.domio.stockcontrol.category.Category;
import co.domio.stockcontrol.category.CategoryRepository;
import co.domio.stockcontrol.common.exception.BusinessRuleException;
import co.domio.stockcontrol.common.exception.DuplicateResourceException;
import co.domio.stockcontrol.common.exception.ResourceNotFoundException;
import co.domio.stockcontrol.movement.MovementType;
import co.domio.stockcontrol.movement.StockMovement;
import co.domio.stockcontrol.movement.StockMovementRepository;
import co.domio.stockcontrol.product.dto.ProductRequest;
import co.domio.stockcontrol.product.dto.ProductResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class ProductServiceIT extends IntegrationTest {

    @Autowired
    ProductService service;
    @Autowired
    ProductRepository productRepository;
    @Autowired
    CategoryRepository categoryRepository;
    @Autowired
    StockMovementRepository movementRepository;

    private Long newCategory() {
        Category c = new Category();
        c.setName("Cat-" + System.nanoTime());
        return categoryRepository.save(c).getId();
    }

    private ProductRequest request(String sku, Long categoryId, Integer stock) {
        return new ProductRequest(sku, "Nombre", null, null, categoryId, null, "unidad", null, stock, 5);
    }

    @Test
    void crea_conStockInicial_yCalculaLowStock() {
        Long cat = newCategory();

        ProductResponse p = service.create(request("SKU-" + System.nanoTime(), cat, 2));

        assertThat(p.stock()).isEqualTo(2);
        assertThat(p.lowStock()).isTrue(); // 2 <= minStock 5
    }

    @Test
    void skuDuplicado_lanzaDuplicate() {
        Long cat = newCategory();
        String sku = "DUP-" + System.nanoTime();
        service.create(request(sku, cat, 0));

        assertThatThrownBy(() -> service.create(request(sku, cat, 0)))
                .isInstanceOf(DuplicateResourceException.class);
    }

    @Test
    void categoriaInexistente_lanzaNotFound() {
        assertThatThrownBy(() -> service.create(request("SKU-" + System.nanoTime(), 999999L, 0)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void actualizar_noModificaElStock() {
        Long cat = newCategory();
        ProductResponse created = service.create(request("UPD-" + System.nanoTime(), cat, 50));

        // Se envia stock 999 en la actualizacion; debe ignorarse.
        ProductRequest update = new ProductRequest(created.sku(), "Nuevo nombre", null, null, cat, null,
                "unidad", null, 999, 10);
        ProductResponse updated = service.update(created.id(), update);

        assertThat(updated.name()).isEqualTo("Nuevo nombre");
        assertThat(updated.minStock()).isEqualTo(10);
        assertThat(updated.stock()).isEqualTo(50); // sin cambios
    }

    @Test
    void crea_conStockInicial_registraMovimientoDeAperturaDesdeCero() {
        Long cat = newCategory();

        ProductResponse created = service.create(request("OPEN-" + System.nanoTime(), cat, 8));

        List<StockMovement> movements = movementRepository.findByProductIdOrderByIdDesc(created.id());
        assertThat(movements).hasSize(1);
        StockMovement opening = movements.get(0);
        assertThat(opening.getMovementType()).isEqualTo(MovementType.IN);
        assertThat(opening.getQuantity()).isEqualTo(8);
        assertThat(opening.getPreviousStock()).isZero();
        assertThat(opening.getNewStock()).isEqualTo(8);
        assertThat(opening.getNote()).isEqualTo(ProductService.OPENING_NOTE);
    }

    @Test
    void crea_conStockCero_noRegistraMovimientoDeApertura() {
        Long cat = newCategory();

        ProductResponse created = service.create(request("ZERO-" + System.nanoTime(), cat, 0));

        assertThat(movementRepository.findByProductIdOrderByIdDesc(created.id())).isEmpty();
    }

    @Test
    void eliminar_conMovimientos_lanzaBusinessRule_yConservaProducto() {
        Long cat = newCategory();
        ProductResponse created = service.create(request("DEL-" + System.nanoTime(), cat, 3));
        assertThat(movementRepository.existsByProductId(created.id())).isTrue(); // apertura

        assertThatThrownBy(() -> service.delete(created.id()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("movimientos");

        assertThat(movementRepository.existsByProductId(created.id())).isTrue();
    }

    @Test
    void eliminar_sinMovimientos_borraFisicamente() {
        Long cat = newCategory();
        ProductResponse created = service.create(request("DEL0-" + System.nanoTime(), cat, 0));

        service.delete(created.id());

        assertThat(productRepository.findById(created.id())).isEmpty();
    }

    @Test
    void archivar_desactivaSinPerderHistorial_yRestaurar_reactiva() {
        Long cat = newCategory();
        ProductResponse created = service.create(request("ARC-" + System.nanoTime(), cat, 5));

        ProductResponse archived = service.archive(created.id());
        assertThat(archived.active()).isFalse();
        assertThat(movementRepository.existsByProductId(created.id())).isTrue();

        ProductResponse restored = service.restore(created.id());
        assertThat(restored.active()).isTrue();
    }

    @Test
    void buscar_porNombreSkuCategoriaYBajoStock_paginaEnElServidor() {
        Long cat = newCategory();
        String suffix = String.valueOf(System.nanoTime());
        service.create(request("SEA1-" + suffix, cat, 10));  // nombre "Nombre"
        service.create(new ProductRequest("SEA2-" + suffix, "Camara especial", null, null, cat, null,
                "unidad", null, 10, 2));
        service.create(new ProductRequest("SEA3-" + suffix, "Sensor especial", null, null, cat, null,
                "unidad", null, 1, 2)); // bajo stock (1 <= 2)

        var porTexto = service.search("especial", cat, null, PageRequest.of(0, 10));
        assertThat(porTexto.totalElements()).isEqualTo(2);

        // La busqueda por texto es insensible a mayusculas: 'sea2' encuentra el SKU 'SEA2-...'.
        var porSku = service.search("sea2-" + suffix, null, null, PageRequest.of(0, 10));
        assertThat(porSku.totalElements()).isEqualTo(1);
        assertThat(porSku.content().get(0).sku()).isEqualTo("SEA2-" + suffix);

        var bajoStock = service.search(null, cat, true, PageRequest.of(0, 10));
        assertThat(bajoStock.totalElements()).isEqualTo(1);
        assertThat(bajoStock.content().get(0).sku()).isEqualTo("SEA3-" + suffix);

        var paginado = service.search("especial", null, null, PageRequest.of(0, 1));
        assertThat(paginado.totalPages()).isEqualTo(2);
        assertThat(paginado.content()).hasSize(1);
    }
}
