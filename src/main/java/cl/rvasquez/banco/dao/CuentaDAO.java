package cl.rvasquez.banco.dao;

import cl.rvasquez.banco.modelo.Cuenta;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Contrato CRUD sobre la entidad {@link Cuenta}.
 *
 * Eliminación: soft delete (UPDATE activa = false). En banca no se borra
 * historia; las cuentas inactivas siguen disponibles para auditoría.
 *
 * Sobrecarga transaccional: {@link #actualizar(Cuenta, Connection)} permite
 * reusar una conexión externa. La usa TransferenciaService (Fase 2) para
 * encadenar débito + crédito + INSERT en transaccion bajo una única
 * transacción JDBC. Contrato: el método NO cierra la conexión recibida; eso
 * lo hace quien la abrió.
 */
public interface CuentaDAO {

    Cuenta insertar(Cuenta cuenta) throws SQLException;

    /** Variante autónoma: abre y cierra su propia conexión. */
    Cuenta actualizar(Cuenta cuenta) throws SQLException;

    /** Variante transaccional: usa la conexión recibida y NO la cierra. */
    Cuenta actualizar(Cuenta cuenta, Connection conn) throws SQLException;

    /** Soft delete: marca activa = false. */
    boolean eliminar(Long id) throws SQLException;

    Optional<Cuenta> buscarPorId(Long id) throws SQLException;

    /** Todas las cuentas, incluyendo las marcadas como inactivas. */
    List<Cuenta> listar() throws SQLException;

    /** Solo cuentas con activa = true. */
    List<Cuenta> listarActivas() throws SQLException;

    /** Todas las cuentas (activas e inactivas) de un cliente. */
    List<Cuenta> buscarPorCliente(Long clienteId) throws SQLException;

    /** Solo cuentas activas de un cliente. */
    List<Cuenta> buscarActivasPorCliente(Long clienteId) throws SQLException;
}
