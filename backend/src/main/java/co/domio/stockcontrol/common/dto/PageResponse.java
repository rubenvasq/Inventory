package co.domio.stockcontrol.common.dto;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * Envoltorio de pagina para los listados paginados del lado del servidor.
 * Expone los metadatos que necesita la interfaz para navegar resultados.
 *
 * @param <T> tipo de los elementos de la pagina
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
