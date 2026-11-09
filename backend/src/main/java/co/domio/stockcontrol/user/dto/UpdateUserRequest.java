package co.domio.stockcontrol.user.dto;

import co.domio.stockcontrol.user.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// La contrasena no lleva @Size porque puede venir vacia (= no cambiar); su
// longitud se valida en el servicio solo cuando trae valor.

/**
 * Actualiza un usuario. La contrasena es opcional: si viene vacia o nula, no se
 * cambia; si viene con valor, se reemplaza.
 */
public record UpdateUserRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
        String fullName,

        @NotNull(message = "El rol es obligatorio")
        Role role,

        Boolean enabled,

        String password) {
}
