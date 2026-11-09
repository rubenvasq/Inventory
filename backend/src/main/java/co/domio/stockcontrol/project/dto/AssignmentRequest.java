package co.domio.stockcontrol.project.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Datos para asignar (o ampliar la asignacion de) un producto a un proyecto.
 */
public record AssignmentRequest(
        @NotNull(message = "El producto es obligatorio") Long productId,
        @NotNull(message = "La cantidad es obligatoria")
        @Positive(message = "La cantidad asignada debe ser mayor que cero") Integer quantity) {
}
