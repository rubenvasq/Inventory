package co.domio.stockcontrol.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Actualiza el perfil del usuario autenticado. Solo el nombre completo: el
 * nombre de usuario, el rol y el estado se gestionan desde la administracion
 * de usuarios (ADMIN).
 */
public record UpdateProfileRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
        String fullName) {
}
