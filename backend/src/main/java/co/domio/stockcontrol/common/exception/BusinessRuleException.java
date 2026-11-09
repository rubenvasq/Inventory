package co.domio.stockcontrol.common.exception;

/**
 * Se lanza cuando una operacion es valida sintacticamente pero viola una regla
 * de negocio (p. ej. una salida que dejaria el stock negativo). El manejador
 * central la traduce a una respuesta HTTP 422.
 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
