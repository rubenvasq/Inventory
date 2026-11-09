package co.domio.stockcontrol.common.exception;

/**
 * Se lanza al intentar crear o actualizar un recurso que violaria una
 * restriccion de unicidad (p. ej. nombre repetido). El manejador central la
 * traduce a una respuesta HTTP 409.
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
