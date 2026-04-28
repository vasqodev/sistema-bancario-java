package cl.rvasquez.banco.servicio;

import cl.rvasquez.banco.dao.ClienteDAO;
import cl.rvasquez.banco.dao.CuentaDAO;
import cl.rvasquez.banco.dao.impl.ClienteDAOImpl;
import cl.rvasquez.banco.dao.impl.CuentaDAOImpl;
import cl.rvasquez.banco.excepcion.ClienteTieneCuentasException;
import cl.rvasquez.banco.modelo.Cliente;
import cl.rvasquez.banco.modelo.CuentaCorriente;
import cl.rvasquez.banco.util.ConexionBD;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests de las reglas que viven en {@link ClienteService} más allá del
 * pass-through al DAO. Convención: una clase de test por production class.
 */
class ClienteServiceTest {

    private final ClienteDAO clienteDAO = new ClienteDAOImpl();
    private final CuentaDAO cuentaDAO = new CuentaDAOImpl();
    private final ClienteService clienteService = new ClienteService(clienteDAO);

    @BeforeEach
    void resetearBD() throws SQLException {
        try (Connection c = ConexionBD.obtenerConexion();
             Statement s = c.createStatement()) {
            s.execute("TRUNCATE TABLE transaccion, cuenta, cliente RESTART IDENTITY CASCADE");
        }
    }

    @Test
    void eliminarClienteConCuentasLanzaClienteTieneCuentasException() throws SQLException {
        Cliente cliente = clienteDAO.insertar(
                new Cliente(null, "11.111.111-1", "Test", "User", "test@example.cl", null, null));
        cuentaDAO.insertar(new CuentaCorriente(
                null, "CTA-X", new BigDecimal("1000.0000"), cliente.getId(), null, true));

        ClienteTieneCuentasException ex = assertThrows(ClienteTieneCuentasException.class,
                () -> clienteService.eliminar(cliente.getId()));
        assertTrue(ex.getMessage().contains("cuentas"),
                "el mensaje debe mencionar la causa para que sea accionable");

        // Cliente debe seguir existiendo: la traducción no oculta el rollback de la BD.
        assertTrue(clienteDAO.buscarPorId(cliente.getId()).isPresent());
    }

    @Test
    void eliminarClienteSinCuentasFunciona() throws SQLException {
        Cliente cliente = clienteDAO.insertar(
                new Cliente(null, "22.222.222-2", "Sin", "Cuentas", "sin@example.cl", null, null));

        assertTrue(clienteService.eliminar(cliente.getId()),
                "el camino feliz debe seguir devolviendo true como antes");
        assertFalse(clienteDAO.buscarPorId(cliente.getId()).isPresent());
    }
}
