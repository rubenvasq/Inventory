package co.domio.stockcontrol.common.exception;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Cuerpo estandar de las respuestas de error de la API.
 *
 * @param timestamp momento en que se genero el error
 * @param status    codigo HTTP
 * @param error     descripcion corta del codigo HTTP
 * @param message   mensaje legible para el cliente
 * @param path      ruta que origino el error
 * @param details   detalles por campo (p. ej. errores de validacion); puede ser null
 */
public record ApiError(
        OffsetDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldValidationError> details) {

    public record FieldValidationError(String field, String message) {
    }

    public static ApiError of(int status, String error, String message, String path) {
        return new ApiError(OffsetDateTime.now(), status, error, message, path, null);
    }

    public static ApiError of(int status, String error, String message, String path,
            List<FieldValidationError> details) {
        return new ApiError(OffsetDateTime.now(), status, error, message, path, details);
    }
}
