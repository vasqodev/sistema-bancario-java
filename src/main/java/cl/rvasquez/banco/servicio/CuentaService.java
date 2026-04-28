package cl.rvasquez.banco.servicio;

import cl.rvasquez.banco.dao.ClienteDAO;
import cl.rvasquez.banco.dao.CuentaDAO;
import cl.rvasquez.banco.excepcion.ClienteNoExisteException;
import cl.rvasquez.banco.modelo.Cuenta;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Capa de servicio sobre {@link CuentaDAO}.
 *
 * Regla de negocio: {@link #crear(Cuenta)} valida que el clienteId referenciado
 * exista antes de insertar. Sin esta validación, una FK violation subiría como
 * SQLException con SQLState 23503 — semánticamente correcta pero opaca para la
 * vista. Atajamos el caso aquí y lanzamos {@link ClienteNoExisteException},
 * que es la misma excepción que usaría TransferenciaService si quisiera el
 * análogo sobre cuentas, manteniendo el patrón nominal de excepciones de dominio.
 */
public class CuentaService {

    private final CuentaDAO cuentaDAO;
    private final ClienteDAO clienteDAO;

    public CuentaService(CuentaDAO cuentaDAO, ClienteDAO clienteDAO) {
        this.cuentaDAO = cuentaDAO;
        this.clienteDAO = clienteDAO;
    }

    public Cuenta crear(Cuenta cuenta) throws SQLException {
        if (clienteDAO.buscarPorId(cuenta.getClienteId()).isEmpty()) {
            throw new ClienteNoExisteException(
                    "No se puede crear la cuenta: el cliente " +
                    cuenta.getClienteId() + " no existe");
        }
        return cuentaDAO.insertar(cuenta);
    }

    public Cuenta actualizar(Cuenta cuenta) throws SQLException {
        return cuentaDAO.actualizar(cuenta);
    }

    /** Soft delete: marca la cuenta como inactiva. */
    public boolean eliminar(Long id) throws SQLException {
        return cuentaDAO.eliminar(id);
    }

    public Optional<Cuenta> buscarPorId(Long id) throws SQLException {
        return cuentaDAO.buscarPorId(id);
    }

    public List<Cuenta> listar() throws SQLException {
        return cuentaDAO.listar();
    }

    public List<Cuenta> listarActivas() throws SQLException {
        return cuentaDAO.listarActivas();
    }

    public List<Cuenta> buscarPorCliente(Long clienteId) throws SQLException {
        return cuentaDAO.buscarPorCliente(clienteId);
    }

    public List<Cuenta> buscarActivasPorCliente(Long clienteId) throws SQLException {
        return cuentaDAO.buscarActivasPorCliente(clienteId);
    }
}
