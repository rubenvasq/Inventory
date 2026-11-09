package co.domio.stockcontrol.movement;

/**
 * Tipo de movimiento de inventario.
 *
 * <ul>
 *   <li>{@code IN} — entrada: suma {@code quantity} al stock.</li>
 *   <li>{@code OUT} — salida: resta {@code quantity} al stock (no puede quedar negativo).</li>
 *   <li>{@code ADJUSTMENT} — ajuste: fija el stock al valor absoluto {@code quantity}
 *       (correccion por conteo fisico).</li>
 * </ul>
 */
public enum MovementType {
    IN,
    OUT,
    ADJUSTMENT
}
