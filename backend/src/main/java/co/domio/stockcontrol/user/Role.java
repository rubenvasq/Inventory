package co.domio.stockcontrol.user;

/**
 * Rol de un usuario.
 *
 * <ul>
 *   <li>{@code ADMIN} — acceso total, incluida la gestion de usuarios.</li>
 *   <li>{@code OPERADOR} — gestiona el inventario (productos, movimientos, etc.)
 *       pero no administra usuarios.</li>
 * </ul>
 */
public enum Role {
    ADMIN,
    OPERADOR
}
