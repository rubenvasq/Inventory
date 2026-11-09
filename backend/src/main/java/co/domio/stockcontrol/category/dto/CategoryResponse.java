package co.domio.stockcontrol.category.dto;

import co.domio.stockcontrol.category.Category;
import java.time.OffsetDateTime;

/**
 * Representacion de salida de una categoria.
 */
public record CategoryResponse(
        Long id,
        String name,
        String description,
        boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt) {

    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getDescription(),
                category.isActive(),
                category.getCreatedAt(),
                category.getUpdatedAt());
    }
}
