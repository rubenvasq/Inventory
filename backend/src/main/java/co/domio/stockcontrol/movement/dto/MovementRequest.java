package co.domio.stockcontrol.movement.dto;

import co.domio.stockcontrol.movement.MovementType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Datos de entrada para registrar un movimiento de inventario.
 *
 * <p>Para {@code IN} y {@code OUT}, {@code quantity} es la cantidad a sumar o
 * restar (debe ser mayor que cero). Para {@code ADJUSTMENT}, {@code quantity} es
 * el nuevo valor absoluto de existencias (puede ser cero).
 */
public record MovementRequest(
        @NotNull(message = "El tipo de movimiento es obligatorio")
        MovementType type,

        @NotNull(message = "La cantidad es obligatoria")
        @PositiveOrZero(message = "La cantidad no puede ser negativa")
        Integer quantity,

        @Size(max = 255, message = "La nota no puede superar 255 caracteres")
        String note) {
}
