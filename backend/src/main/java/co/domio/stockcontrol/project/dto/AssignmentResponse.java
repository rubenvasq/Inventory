package co.domio.stockcontrol.project.dto;

import co.domio.stockcontrol.project.ProjectAssignment;
import java.time.OffsetDateTime;

/**
 * Representacion de salida de una asignacion de material a un proyecto.
 */
public record AssignmentResponse(
        Long id,
        Long productId,
        String productSku,
        String productName,
        String unit,
        int assignedQuantity,
        int deliveredQuantity,
        int returnedQuantity,
        int pendingQuantity,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {

    public static AssignmentResponse from(ProjectAssignment assignment) {
        var product = assignment.getProduct();
        return new AssignmentResponse(
                assignment.getId(),
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getUnit(),
                assignment.getAssignedQuantity(),
                assignment.getDeliveredQuantity(),
                assignment.getReturnedQuantity(),
                assignment.pendingQuantity(),
                assignment.getCreatedAt(),
                assignment.getUpdatedAt());
    }
}
