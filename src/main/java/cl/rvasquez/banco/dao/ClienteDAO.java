package cl.rvasquez.banco.dao;

import cl.rvasquez.banco.modelo.Cliente;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Contrato CRUD sobre la entidad {@link Cliente}.
 *
 * Las firmas declaran SQLException porque la app es JDBC puro y queremos
 * que la capa superior (servicio o vista) decida cómo presentar errores
 * de base de datos al usuario (típicamente un JOptionPane).
 *
 * Eliminación: hard delete. Si el cliente tiene cuentas, la FK lo bloquea
 * y la SQLException sube con el detalle de violación de integridad.
 */
public interface ClienteDAO {

    /**
     * Inserta un cliente nuevo (id == null) y devuelve la misma instancia
     * con id y fechaCreacion completados desde la cláusula RETURNING.
     */
    Cliente insertar(Cliente cliente) throws SQLException;

    Cliente actualizar(Cliente cliente) throws SQLException;

    boolean eliminar(Long id) throws SQLException;

    Optional<Cliente> buscarPorId(Long id) throws SQLException;

    List<Cliente> listar() throws SQLException;
}
