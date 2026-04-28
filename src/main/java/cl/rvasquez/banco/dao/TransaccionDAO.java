package cl.rvasquez.banco.dao;

import cl.rvasquez.banco.modelo.Transaccion;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * Contrato sobre la tabla de auditoría de transferencias.
 *
 * No expone {@code eliminar}: las transacciones bancarias no se borran. No
 * expone {@code actualizar}: una transacción es inmutable una vez registrada.
 *
 * Sobrecarga transaccional: {@link #insertar(Transaccion, Connection)} es la
 * que usa TransferenciaService para registrar el evento dentro de la misma
 * transacción JDBC que aplica el débito y el crédito.
 */
public interface TransaccionDAO {

    /** Variante autónoma: abre y cierra su propia conexión. */
    Transaccion insertar(Transaccion transaccion) throws SQLException;

    /** Variante transaccional: usa la conexión recibida y NO la cierra. */
    Transaccion insertar(Transaccion transaccion, Connection conn) throws SQLException;

    /** Historial de movimientos (origen O destino) de una cuenta, más recientes primero. */
    List<Transaccion> listarPorCuenta(Long cuentaId) throws SQLException;

    /** Todas las transacciones registradas, más recientes primero. */
    List<Transaccion> listar() throws SQLException;
}
