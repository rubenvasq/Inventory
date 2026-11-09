package co.domio.stockcontrol.product.dto;

import co.domio.stockcontrol.product.Product;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Representacion de salida de un producto, con datos basicos de su categoria y
 * proveedor para evitar llamadas adicionales desde el cliente.
 */
public record ProductResponse(
        Long id,
        String sku,
        String name,
        String description,
        String imageUrl,
        Long categoryId,
        String categoryName,
        Long supplierId,
        String supplierName,
        String unit,
        BigDecimal unitPrice,
        int stock,
        int minStock,
        boolean lowStock,
        boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {

    public static ProductResponse from(Product product) {
        var supplier = product.getSupplier();
        return new ProductResponse(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getDescription(),
                product.getImageUrl(),
                product.getCategory().getId(),
                product.getCategory().getName(),
                supplier != null ? supplier.getId() : null,
                supplier != null ? supplier.getName() : null,
                product.getUnit(),
                product.getUnitPrice(),
                product.getStock(),
                product.getMinStock(),
                product.getMinStock() > 0 && product.getStock() <= product.getMinStock(),
                product.isActive(),
                product.getCreatedAt(),
                product.getUpdatedAt());
    }
}
