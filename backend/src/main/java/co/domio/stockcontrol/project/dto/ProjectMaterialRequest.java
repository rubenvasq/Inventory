package co.domio.stockcontrol.project.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Datos para entregar material a un proyecto o devolver material de vuelta al
 * inventario.
 */
public record ProjectMaterialRequest(
        @NotNull(message = "El producto es obligatorio") Long productId,
        @NotNull(message = "La cantidad es obligatoria")
        @Positive(message = "La cantidad debe ser mayor que cero") Integer quantity,
        @Size(max = 255) String note) {
}
