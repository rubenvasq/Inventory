package co.domio.stockcontrol.supplier.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Datos de entrada para crear o actualizar un proveedor.
 */
public record SupplierRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
        String name,

        @Size(max = 150, message = "El nombre de contacto no puede superar 150 caracteres")
        String contactName,

        @Email(message = "El correo no tiene un formato valido")
        @Size(max = 150, message = "El correo no puede superar 150 caracteres")
        String email,

        @Size(max = 50, message = "El telefono no puede superar 50 caracteres")
        String phone,

        @Size(max = 255, message = "La direccion no puede superar 255 caracteres")
        String address,

        Boolean active) {
}
