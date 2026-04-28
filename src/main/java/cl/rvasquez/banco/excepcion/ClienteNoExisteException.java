package cl.rvasquez.banco.excepcion;

/**
 * Se lanza cuando una operación referencia un id de cliente que no existe en
 * la BD. Usada por CuentaService al validar clienteId antes de insertar una
 * cuenta nueva, así se rechaza el caso en la capa de servicio en lugar de
 * propagar una FK violation desde la BD.
 */
public class ClienteNoExisteException extends RuntimeException {

    public ClienteNoExisteException(String mensaje) {
        super(mensaje);
    }
}
