package co.domio.stockcontrol.product;

import co.domio.stockcontrol.category.Category;
import co.domio.stockcontrol.category.CategoryRepository;
import co.domio.stockcontrol.common.exception.DuplicateResourceException;
import co.domio.stockcontrol.common.exception.ResourceNotFoundException;
import co.domio.stockcontrol.product.dto.ProductRequest;
import co.domio.stockcontrol.product.dto.ProductResponse;
import co.domio.stockcontrol.supplier.Supplier;
import co.domio.stockcontrol.supplier.SupplierRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Logica de negocio para productos y existencias.
 */
@Service
@Transactional
public class ProductService {

    private static final String RESOURCE = "Producto";

    private final ProductRepository repository;
    private final CategoryRepository categoryRepository;
    private final SupplierRepository supplierRepository;

    public ProductService(ProductRepository repository, CategoryRepository categoryRepository,
            SupplierRepository supplierRepository) {
        this.repository = repository;
        this.categoryRepository = categoryRepository;
        this.supplierRepository = supplierRepository;
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> findAll(Long categoryId) {
        List<Product> products = (categoryId != null)
                ? repository.findByCategoryId(categoryId)
                : repository.findAll();
        return products.stream().map(ProductResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> findLowStock() {
        return repository.findLowStock().stream().map(ProductResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse findById(Long id) {
        return repository.findById(id)
                .map(ProductResponse::from)
                .orElseThrow(() -> ResourceNotFoundException.of(RESOURCE, id));
    }

    public ProductResponse create(ProductRequest request) {
        if (repository.existsBySkuIgnoreCase(request.sku())) {
            throw new DuplicateResourceException("Ya existe un producto con el SKU '%s'".formatted(request.sku()));
        }
        Product product = new Product();
        apply(product, request);
        // El stock solo se fija en la creacion; despues se gestiona con movimientos.
        product.setStock(request.stock() != null ? request.stock() : 0);
        return ProductResponse.from(repository.save(product));
    }

    public ProductResponse update(Long id, ProductRequest request) {
        Product product = repository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of(RESOURCE, id));
        repository.findBySkuIgnoreCase(request.sku())
                .filter(existing -> !existing.getId().equals(id))
                .ifPresent(existing -> {
                    throw new DuplicateResourceException(
                            "Ya existe un producto con el SKU '%s'".formatted(request.sku()));
                });
        apply(product, request);
        // El stock no se modifica en la actualizacion (ver movimientos, Etapa 4).
        return ProductResponse.from(repository.save(product));
    }

    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw ResourceNotFoundException.of(RESOURCE, id);
        }
        repository.deleteById(id);
    }

    private void apply(Product product, ProductRequest request) {
        product.setSku(request.sku().trim());
        product.setName(request.name().trim());
        product.setDescription(request.description());
        product.setImageUrl(request.imageUrl());
        product.setCategory(resolveCategory(request.categoryId()));
        product.setSupplier(resolveSupplier(request.supplierId()));
        if (request.unit() != null && !request.unit().isBlank()) {
            product.setUnit(request.unit().trim());
        }
        product.setUnitPrice(request.unitPrice());
        if (request.minStock() != null) {
            product.setMinStock(request.minStock());
        }
    }

    private Category resolveCategory(Long categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> ResourceNotFoundException.of("Categoria", categoryId));
    }

    private Supplier resolveSupplier(Long supplierId) {
        if (supplierId == null) {
            return null;
        }
        return supplierRepository.findById(supplierId)
                .orElseThrow(() -> ResourceNotFoundException.of("Proveedor", supplierId));
    }
}
