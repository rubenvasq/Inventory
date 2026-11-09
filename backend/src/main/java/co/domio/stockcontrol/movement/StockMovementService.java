package co.domio.stockcontrol.movement;

import co.domio.stockcontrol.common.exception.BusinessRuleException;
import co.domio.stockcontrol.common.exception.ResourceNotFoundException;
import co.domio.stockcontrol.movement.dto.MovementRequest;
import co.domio.stockcontrol.movement.dto.MovementResponse;
import co.domio.stockcontrol.product.Product;
import co.domio.stockcontrol.product.ProductRepository;
import java.util.List;
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

    public StockMovementService(StockMovementRepository movementRepository, ProductRepository productRepository) {
        this.movementRepository = movementRepository;
        this.productRepository = productRepository;
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
            case ADJUSTMENT -> quantity; // nuevo valor absoluto de existencias
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
        return movementRepository.findByProductIdOrderByIdDesc(productId).stream()
                .map(MovementResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<MovementResponse> findAll(MovementType type) {
        List<StockMovement> movements = (type != null)
                ? movementRepository.findByMovementTypeOrderByIdDesc(type)
                : movementRepository.findAllByOrderByIdDesc();
        return movements.stream().map(MovementResponse::from).toList();
    }

    private void requirePositive(int quantity) {
        if (quantity <= 0) {
            throw new BusinessRuleException("La cantidad de una entrada o salida debe ser mayor que cero");
        }
    }
}
