package cl.rvasquez.banco.excepcion;

/**
 * Se lanza cuando una operación de débito o crédito apunta a una cuenta
 * marcada como inactiva (eliminada lógicamente).
 */
public class CuentaInactivaException extends RuntimeException {

    public CuentaInactivaException(String mensaje) {
        super(mensaje);
    }
}
