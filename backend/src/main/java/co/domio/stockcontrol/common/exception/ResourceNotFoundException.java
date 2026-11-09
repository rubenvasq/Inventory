package co.domio.stockcontrol.common.exception;

/**
 * Se lanza cuando un recurso solicitado no existe. El manejador central la
 * traduce a una respuesta HTTP 404.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public static ResourceNotFoundException of(String resource, Object id) {
        return new ResourceNotFoundException("No se encontro %s con id %s".formatted(resource, id));
    }
}
