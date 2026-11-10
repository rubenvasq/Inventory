package co.domio.stockcontrol.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Cambia la contrasena del usuario autenticado. Exige la contrasena actual
 * para evitar cambios no autorizados con un token prestado o robado.
 */
public record ChangePasswordRequest(
        @NotBlank(message = "La contrasena actual es obligatoria")
        String currentPassword,

        @NotBlank(message = "La nueva contrasena es obligatoria")
        @Size(min = 6, max = 100, message = "La nueva contrasena debe tener entre 6 y 100 caracteres")
        String newPassword) {
}
