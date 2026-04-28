package cl.rvasquez.banco.excepcion;

/**
 * Se lanza cuando se intenta debitar de una cuenta un monto mayor que su saldo.
 * Excepción de negocio (unchecked): la captura el servicio para hacer rollback
 * de la transacción JDBC y la UI para mostrar el mensaje al usuario.
 */
public class SaldoInsuficienteException extends RuntimeException {

    public SaldoInsuficienteException(String mensaje) {
        super(mensaje);
    }
}
