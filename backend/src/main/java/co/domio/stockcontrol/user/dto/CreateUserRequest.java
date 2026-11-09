package co.domio.stockcontrol.user.dto;

import co.domio.stockcontrol.user.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank(message = "El usuario es obligatorio")
        @Size(max = 60, message = "El usuario no puede superar 60 caracteres")
        String username,

        @NotBlank(message = "La contrasena es obligatoria")
        @Size(min = 6, max = 72, message = "La contrasena debe tener entre 6 y 72 caracteres")
        String password,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
        String fullName,

        @NotNull(message = "El rol es obligatorio")
        Role role,

        Boolean enabled) {
}
