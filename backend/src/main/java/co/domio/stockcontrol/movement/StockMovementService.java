package co.domio.stockcontrol.movement;

import co.domio.stockcontrol.common.dto.PageResponse;
import co.domio.stockcontrol.common.exception.BusinessRuleException;
import co.domio.stockcontrol.common.exception.ResourceNotFoundException;
import co.domio.stockcontrol.movement.dto.MovementRequest;
import co.domio.stockcontrol.movement.dto.MovementResponse;
import co.domio.stockcontrol.product.Product;
import co.domio.stockcontrol.product.ProductRepository;
import co.domio.stockcontrol.project.Project;
import co.domio.stockcontrol.project.ProjectRepository;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registra movimientos de inventario y actualiza, de forma atomica, las
 * existencias del producto afectado.
 */
@Service
@Transactional
public class StockMovementService {

    private final StockMovementRepository movementRepository;
    private final ProductRepository productRepository;
    private final ProjectRepository projectRepository;

    public StockMovementService(StockMovementRepository movementRepository, ProductRepository productRepository,
            ProjectRepository projectRepository) {
        this.movementRepository = movementRepository;
        this.productRepository = productRepository;
        this.projectRepository = projectRepository;
    }

    public MovementResponse register(Long productId, MovementRequest request) {
        // Bloqueo de escritura: evita que dos movimientos concurrentes dejen el stock inconsistente.
        Product product = productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> ResourceNotFoundException.of("Producto", productId));

        int previousStock = product.getStock();
        int quantity = request.quantity();
        int newStock = switch (request.type()) {
            case IN -> {
                requirePositive(quantity);
                yield previousStock + quantity;
            }
            case OUT -> {
                requirePositive(quantity);
                if (quantity > previousStock) {
                    throw new BusinessRuleException(
                            "Stock insuficiente: existencias actuales %d, salida solicitada %d"
                                    .formatted(previousStock, quantity));
                }
                yield previousStock - quantity;
            }
            case ADJUSTMENT -> requireNonNegative(quantity); // nuevo valor absoluto de existencias
        };

        product.setStock(newStock);
        productRepository.save(product);

        StockMovement movement = new StockMovement();
        movement.setProduct(product);
        movement.setMovementType(request.type());
        movement.setQuantity(quantity);
        movement.setPreviousStock(previousStock);
        movement.setNewStock(newStock);
        movement.setNote(request.note());

        return MovementResponse.from(movementRepository.save(movement));
    }

    @Transactional(readOnly = true)
    public List<MovementResponse> findByProduct(Long productId) {
        if (!productRepository.existsById(productId)) {
            throw ResourceNotFoundException.of("Producto", productId);
        }
        List<StockMovement> movements = movementRepository.findByProductIdOrderByIdDesc(productId);
        return toResponses(movements);
    }

    /**
     * Busqueda paginada de movimientos con filtros opcionales por producto
     * (id o texto contra nombre/SKU), tipo y rango de fechas de registro.
     */
    @Transactional(readOnly = true)
    public PageResponse<MovementResponse> search(Long productId, String productSearch, MovementType type,
            LocalDate from, LocalDate to, Pageable pageable) {
        if (productId != null && !productRepository.existsById(productId)) {
            throw ResourceNotFoundException.of("Producto", productId);
        }

        String pattern = likePattern(productSearch);
        Specification<StockMovement> spec = (root, query, cb) -> {
            List<Predicate> predicates = new java.util.ArrayList<>();
            if (productId != null) {
                predicates.add(cb.equal(root.get("product").get("id"), productId));
            }
            if (pattern != null) {
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("product").get("name")), pattern),
                        cb.like(cb.lower(root.get("product").get("sku")), pattern)));
            }
            if (type != null) {
                predicates.add(cb.equal(root.get("movementType"), type));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"),
                        from.atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime()));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"),
                        to.atTime(LocalTime.MAX).atZone(ZoneId.systemDefault()).toOffsetDateTime()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<StockMovement> page = movementRepository.findAll(spec, pageable);
        Map<Long, String> projectNames = projectNames(page.getContent());
        return PageResponse.from(page.map(m -> toResponse(m, projectNames)));
    }

    @Transactional(readOnly = true)
    public List<MovementResponse> findByProject(Long projectId) {
        return toResponses(movementRepository.findByProjectIdOrderByIdDesc(projectId));
    }

    private List<MovementResponse> toResponses(List<StockMovement> movements) {
        Map<Long, String> projectNames = projectNames(movements);
        return movements.stream().map(m -> toResponse(m, projectNames)).toList();
    }

    private MovementResponse toResponse(StockMovement movement, Map<Long, String> projectNames) {
        String projectName = movement.getProjectId() == null ? null : projectNames.get(movement.getProjectId());
        return MovementResponse.from(movement, projectName);
    }

    /** Nombres de proyecto de los movimientos dados, en una sola consulta. */
    private Map<Long, String> projectNames(List<StockMovement> movements) {
        List<Long> projectIds = movements.stream()
                .map(StockMovement::getProjectId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (projectIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, String> names = new HashMap<>();
        for (Project project : projectRepository.findAllById(projectIds)) {
            names.put(project.getId(), project.getName());
        }
        return names;
    }

    private String likePattern(String search) {
        if (search == null || search.isBlank()) {
            return null;
        }
        return "%" + search.trim().toLowerCase() + "%";
    }

    private void requirePositive(int quantity) {
        if (quantity <= 0) {
            throw new BusinessRuleException("La cantidad de una entrada o salida debe ser mayor que cero");
        }
    }

    private int requireNonNegative(int quantity) {
        if (quantity < 0) {
            throw new BusinessRuleException("El nuevo valor de existencias no puede ser negativo");
        }
        return quantity;
    }
}
