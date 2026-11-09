package co.domio.stockcontrol.movement.dto;

import co.domio.stockcontrol.movement.MovementType;
import co.domio.stockcontrol.movement.StockMovement;
import java.time.OffsetDateTime;

/**
 * Representacion de salida de un movimiento de inventario.
 */
public record MovementResponse(
        Long id,
        Long productId,
        String productSku,
        String productName,
        MovementType type,
        int quantity,
        int previousStock,
        int newStock,
        String note,
        Long projectId,
        String projectName,
        OffsetDateTime createdAt) {

    public static MovementResponse from(StockMovement movement) {
        return from(movement, null);
    }

    public static MovementResponse from(StockMovement movement, String projectName) {
        var product = movement.getProduct();
        return new MovementResponse(
                movement.getId(),
                product.getId(),
                product.getSku(),
                product.getName(),
                movement.getMovementType(),
                movement.getQuantity(),
                movement.getPreviousStock(),
                movement.getNewStock(),
                movement.getNote(),
                movement.getProjectId(),
                projectName,
                movement.getCreatedAt());
    }
}
