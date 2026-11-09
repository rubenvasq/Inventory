package co.domio.stockcontrol.movement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.domio.stockcontrol.IntegrationTest;
import co.domio.stockcontrol.category.Category;
import co.domio.stockcontrol.category.CategoryRepository;
import co.domio.stockcontrol.common.exception.BusinessRuleException;
import co.domio.stockcontrol.common.exception.ResourceNotFoundException;
import co.domio.stockcontrol.movement.dto.MovementRequest;
import co.domio.stockcontrol.movement.dto.MovementResponse;
import co.domio.stockcontrol.product.Product;
import co.domio.stockcontrol.product.ProductRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class StockMovementServiceIT extends IntegrationTest {

    @Autowired
    StockMovementService service;
    @Autowired
    ProductRepository productRepository;
    @Autowired
    CategoryRepository categoryRepository;

    private Product newProduct(int stock, int minStock) {
        Category category = new Category();
        category.setName("Cat-" + System.nanoTime());
        categoryRepository.save(category);

        Product product = new Product();
        product.setSku("SKU-" + System.nanoTime());
        product.setName("Producto de prueba");
        product.setCategory(category);
        product.setStock(stock);
        product.setMinStock(minStock);
        return productRepository.save(product);
    }

    @Test
    void entrada_sumaAlStock_yRegistraMovimiento() {
        Product p = newProduct(10, 2);

        MovementResponse mv = service.register(p.getId(), new MovementRequest(MovementType.IN, 5, "compra"));

        assertThat(mv.previousStock()).isEqualTo(10);
        assertThat(mv.newStock()).isEqualTo(15);
        assertThat(productRepository.findById(p.getId()).orElseThrow().getStock()).isEqualTo(15);
    }

    @Test
    void salida_restaDelStock() {
        Product p = newProduct(10, 2);

        MovementResponse mv = service.register(p.getId(), new MovementRequest(MovementType.OUT, 4, null));

        assertThat(mv.newStock()).isEqualTo(6);
        assertThat(productRepository.findById(p.getId()).orElseThrow().getStock()).isEqualTo(6);
    }

    @Test
    void salidaSinExistencias_lanzaBusinessRule_yNoCambiaStock() {
        Product p = newProduct(3, 0);

        assertThatThrownBy(() -> service.register(p.getId(), new MovementRequest(MovementType.OUT, 10, null)))
                .isInstanceOf(BusinessRuleException.class);

        assertThat(productRepository.findById(p.getId()).orElseThrow().getStock()).isEqualTo(3);
    }

    @Test
    void ajuste_fijaValorAbsoluto() {
        Product p = newProduct(10, 2);

        MovementResponse mv = service.register(p.getId(), new MovementRequest(MovementType.ADJUSTMENT, 7, "conteo"));

        assertThat(mv.newStock()).isEqualTo(7);
        assertThat(productRepository.findById(p.getId()).orElseThrow().getStock()).isEqualTo(7);
    }

    @Test
    void entradaConCantidadCero_lanzaBusinessRule() {
        Product p = newProduct(10, 2);

        assertThatThrownBy(() -> service.register(p.getId(), new MovementRequest(MovementType.IN, 0, null)))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void productoInexistente_lanzaNotFound() {
        assertThatThrownBy(() -> service.register(999999L, new MovementRequest(MovementType.IN, 1, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void buscar_filtraPorProductoTextoTipoYFecha_conPaginacion() {
        Product p = newProduct(10, 0);
        service.register(p.getId(), new MovementRequest(MovementType.IN, 5, "compra"));
        service.register(p.getId(), new MovementRequest(MovementType.OUT, 3, "venta"));
        service.register(p.getId(), new MovementRequest(MovementType.ADJUSTMENT, 8, "conteo"));

        var porProducto = service.search(p.getId(), null, null, null, null, PageRequest.of(0, 10));
        assertThat(porProducto.totalElements()).isEqualTo(3);

        var porTexto = service.search(null, p.getName(), null, null, null, PageRequest.of(0, 10));
        assertThat(porTexto.totalElements()).isEqualTo(3);

        var porTipo = service.search(null, null, MovementType.OUT, null, null, PageRequest.of(0, 10));
        assertThat(porTipo.totalElements()).isEqualTo(1);
        assertThat(porTipo.content().get(0).newStock()).isEqualTo(12); // 10 + 5 - 3

        // El rango de fechas se combina con el filtro de producto: sin el, el
        // conteo incluiria los saldos de apertura que creo la migracion V7
        // para los productos del catalogo sembrado.
        var porFecha = service.search(p.getId(), null, null, LocalDate.now(), LocalDate.now(),
                PageRequest.of(0, 10));
        assertThat(porFecha.totalElements()).isEqualTo(3);

        var paginado = service.search(p.getId(), null, null, null, null,
                PageRequest.of(0, 2, Sort.by(Sort.Direction.DESC, "id")));
        assertThat(paginado.content()).hasSize(2);
        assertThat(paginado.totalPages()).isEqualTo(2);
        // Mas recientes primero cuando se pide ese orden (default del endpoint).
        assertThat(paginado.content().get(0).type()).isEqualTo(MovementType.ADJUSTMENT);
    }

    @Test
    void buscarPorProductoInexistente_lanzaNotFound() {
        assertThatThrownBy(() -> service.search(999999L, null, null, null, null, PageRequest.of(0, 10)))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
