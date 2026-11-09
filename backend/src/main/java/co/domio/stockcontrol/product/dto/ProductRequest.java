package co.domio.stockcontrol.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/**
 * Datos de entrada para crear o actualizar un producto.
 *
 * <p>El campo {@code stock} representa las existencias iniciales y solo se tiene
 * en cuenta al crear; en la actualizacion se ignora, ya que a partir de la
 * Etapa 4 las existencias se modifican mediante movimientos de inventario.
 */
public record ProductRequest(
        @NotBlank(message = "El SKU es obligatorio")
        @Size(max = 60, message = "El SKU no puede superar 60 caracteres")
        String sku,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
        String name,

        @Size(max = 500, message = "La descripcion no puede superar 500 caracteres")
        String description,

        @Size(max = 300, message = "La URL de la imagen no puede superar 300 caracteres")
        String imageUrl,

        @NotNull(message = "La categoria es obligatoria")
        Long categoryId,

        Long supplierId,

        @Size(max = 30, message = "La unidad no puede superar 30 caracteres")
        String unit,

        @PositiveOrZero(message = "El precio no puede ser negativo")
        BigDecimal unitPrice,

        @PositiveOrZero(message = "El stock inicial no puede ser negativo")
        Integer stock,

        @PositiveOrZero(message = "El stock minimo no puede ser negativo")
        Integer minStock) {
}
