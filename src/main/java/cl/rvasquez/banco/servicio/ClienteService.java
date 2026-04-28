package cl.rvasquez.banco.servicio;

import cl.rvasquez.banco.dao.ClienteDAO;
import cl.rvasquez.banco.excepcion.ClienteTieneCuentasException;
import cl.rvasquez.banco.modelo.Cliente;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Capa de servicio sobre {@link ClienteDAO}.
 *
 * Regla de presentación: {@link #eliminar(Long)} traduce el SQLState "23503"
 * (foreign_key_violation de PostgreSQL, gatillado por la FK
 * cuenta.cliente_id) en una {@link ClienteTieneCuentasException}. Cualquier
 * otra SQLException se propaga sin alterar — no enmascaramos errores reales
 * de infraestructura (pérdida de conexión, sintaxis, etc.).
 */
public class ClienteService {

    /** SQLState estándar de PostgreSQL para violación de foreign key. */
    private static final String SQLSTATE_FK_VIOLATION = "23503";

    private final ClienteDAO clienteDAO;

    public ClienteService(ClienteDAO clienteDAO) {
        this.clienteDAO = clienteDAO;
    }

    public Cliente crear(Cliente cliente) throws SQLException {
        return clienteDAO.insertar(cliente);
    }

    public Cliente actualizar(Cliente cliente) throws SQLException {
        return clienteDAO.actualizar(cliente);
    }

    public boolean eliminar(Long id) throws SQLException {
        try {
            return clienteDAO.eliminar(id);
        } catch (SQLException e) {
            if (SQLSTATE_FK_VIOLATION.equals(e.getSQLState())) {
                throw new ClienteTieneCuentasException(
                        "No se puede eliminar el cliente porque tiene cuentas asociadas");
            }
            throw e;
        }
    }

    public Optional<Cliente> buscarPorId(Long id) throws SQLException {
        return clienteDAO.buscarPorId(id);
    }

    public List<Cliente> listar() throws SQLException {
        return clienteDAO.listar();
    }
}
