package cl.rvasquez.banco.excepcion;

/**
 * Se lanza al intentar eliminar un cliente que tiene cuentas asociadas.
 * ClienteService la usa para traducir SQLState "23503"
 * (foreign_key_violation de PostgreSQL) en una excepción de dominio nominal,
 * de manera que la vista pueda capturarla específicamente y mostrar un
 * mensaje accionable, sin confundirse con un IllegalStateException genérico.
 */
public class ClienteTieneCuentasException extends RuntimeException {

    public ClienteTieneCuentasException(String mensaje) {
        super(mensaje);
    }
}
