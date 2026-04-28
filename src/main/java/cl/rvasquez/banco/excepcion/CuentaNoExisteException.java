package cl.rvasquez.banco.excepcion;

/**
 * Se lanza cuando una operación referencia un id de cuenta que no existe en la BD.
 * Usada por TransferenciaService al cargar las cuentas origen y destino.
 */
public class CuentaNoExisteException extends RuntimeException {

    public CuentaNoExisteException(String mensaje) {
        super(mensaje);
    }
}
