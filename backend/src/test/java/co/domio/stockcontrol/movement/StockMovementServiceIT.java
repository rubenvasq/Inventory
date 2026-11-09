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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
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
}
