package cl.rvasquez.banco.excepcion;

/**
 * Se lanza cuando un monto de operación es nulo o no positivo.
 * Espejo en código de la restricción CHECK (monto > 0) del schema.
 */
public class MontoInvalidoException extends RuntimeException {

    public MontoInvalidoException(String mensaje) {
        super(mensaje);
    }
}
