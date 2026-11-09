package co.domio.stockcontrol.product;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.domio.stockcontrol.IntegrationTest;
import co.domio.stockcontrol.category.Category;
import co.domio.stockcontrol.category.CategoryRepository;
import co.domio.stockcontrol.common.exception.DuplicateResourceException;
import co.domio.stockcontrol.common.exception.ResourceNotFoundException;
import co.domio.stockcontrol.product.dto.ProductRequest;
import co.domio.stockcontrol.product.dto.ProductResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class ProductServiceIT extends IntegrationTest {

    @Autowired
    ProductService service;
    @Autowired
    CategoryRepository categoryRepository;

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
}
