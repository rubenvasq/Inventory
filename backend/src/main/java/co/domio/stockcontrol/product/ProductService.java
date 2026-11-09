package co.domio.stockcontrol.product;

import co.domio.stockcontrol.category.Category;
import co.domio.stockcontrol.category.CategoryRepository;
import co.domio.stockcontrol.common.dto.PageResponse;
import co.domio.stockcontrol.common.exception.BusinessRuleException;
import co.domio.stockcontrol.common.exception.DuplicateResourceException;
import co.domio.stockcontrol.common.exception.ResourceNotFoundException;
import co.domio.stockcontrol.movement.MovementType;
import co.domio.stockcontrol.movement.StockMovement;
import co.domio.stockcontrol.movement.StockMovementRepository;
import co.domio.stockcontrol.product.dto.ProductRequest;
import co.domio.stockcontrol.product.dto.ProductResponse;
import co.domio.stockcontrol.supplier.Supplier;
import co.domio.stockcontrol.supplier.SupplierRepository;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Logica de negocio para productos y existencias.
 */
@Service
@Transactional
public class ProductService {

    private static final String RESOURCE = "Producto";

    /** Nota fija de los movimientos que explican el saldo inicial de un producto. */
    static final String OPENING_NOTE = "Saldo inicial al crear el producto";

    private final ProductRepository repository;
    private final CategoryRepository categoryRepository;
    private final SupplierRepository supplierRepository;
    private final StockMovementRepository movementRepository;

    public ProductService(ProductRepository repository, CategoryRepository categoryRepository,
            SupplierRepository supplierRepository, StockMovementRepository movementRepository) {
        this.repository = repository;
        this.categoryRepository = categoryRepository;
        this.supplierRepository = supplierRepository;
        this.movementRepository = movementRepository;
    }

    /**
     * Busca productos con paginacion del lado del servidor. El texto de
     * busqueda se compara contra nombre y SKU; los filtros opcionales son
     * categoria y "solo bajo stock".
     */
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> search(String search, Long categoryId, Boolean lowStockOnly,
            Pageable pageable) {
        String pattern = (search == null || search.isBlank())
                ? null
                : "%" + search.trim().toLowerCase() + "%";

        Specification<Product> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (pattern != null) {
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(root.get("sku")), pattern)));
            }
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }
            if (Boolean.TRUE.equals(lowStockOnly)) {
                predicates.add(cb.and(
                        cb.gt(root.get("minStock"), 0),
                        cb.lessThanOrEqualTo(root.get("stock"), root.get("minStock"))));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Product> page = repository.findAll(spec, pageable);
        return PageResponse.from(page.map(ProductResponse::from));
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

    /**
     * Crea un producto y, si trae existencias iniciales, registra en la misma
     * transaccion el movimiento de apertura que explica ese saldo desde cero.
     */
    public ProductResponse create(ProductRequest request) {
        if (repository.existsBySkuIgnoreCase(request.sku())) {
            throw new DuplicateResourceException("Ya existe un producto con el SKU '%s'".formatted(request.sku()));
        }
        Product product = new Product();
        apply(product, request);
        // El stock solo se fija en la creacion; despues se gestiona con movimientos.
        product.setStock(request.stock() != null ? request.stock() : 0);
        product = repository.save(product);

        int initialStock = product.getStock();
        if (initialStock > 0) {
            registerOpeningMovement(product, initialStock);
        }
        return ProductResponse.from(product);
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

    /**
     * Elimina un producto. Si tiene movimientos en su historial, el libro mayor
     * impide el borrado: se lanza un error de negocio para que el producto se
     * archive en su lugar (ver {@link #archive}) y se conserve la trazabilidad.
     */
    public void delete(Long id) {
        Product product = repository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of(RESOURCE, id));
        if (movementRepository.existsByProductId(id)) {
            throw new BusinessRuleException(
                    "El producto '%s' tiene movimientos de inventario asociados y no puede eliminarse; "
                            + "archivalo para conservar su historial".formatted(product.getName()));
        }
        repository.delete(product);
    }

    /** Archiva un producto: deja de estar activo pero conserva su historial. */
    public ProductResponse archive(Long id) {
        return setActive(id, false);
    }

    /** Reactiva un producto archivado. */
    public ProductResponse restore(Long id) {
        return setActive(id, true);
    }

    private ProductResponse setActive(Long id, boolean active) {
        Product product = repository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of(RESOURCE, id));
        product.setActive(active);
        return ProductResponse.from(repository.save(product));
    }

    /** Movimiento de apertura desde stock cero; se ejecuta en la misma transaccion que el alta. */
    private void registerOpeningMovement(Product product, int initialStock) {
        StockMovement movement = new StockMovement();
        movement.setProduct(product);
        movement.setMovementType(MovementType.IN);
        movement.setQuantity(initialStock);
        movement.setPreviousStock(0);
        movement.setNewStock(initialStock);
        movement.setNote(OPENING_NOTE);
        movementRepository.save(movement);
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
