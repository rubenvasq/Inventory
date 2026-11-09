package co.domio.stockcontrol.supplier.dto;

import co.domio.stockcontrol.supplier.Supplier;
import java.time.OffsetDateTime;

/**
 * Representacion de salida de un proveedor.
 */
public record SupplierResponse(
        Long id,
        String name,
        String contactName,
        String email,
        String phone,
        String address,
        boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {

    public static SupplierResponse from(Supplier supplier) {
        return new SupplierResponse(
                supplier.getId(),
                supplier.getName(),
                supplier.getContactName(),
                supplier.getEmail(),
                supplier.getPhone(),
                supplier.getAddress(),
                supplier.isActive(),
                supplier.getCreatedAt(),
                supplier.getUpdatedAt());
    }
}
