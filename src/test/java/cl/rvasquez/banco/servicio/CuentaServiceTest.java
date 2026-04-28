package cl.rvasquez.banco.servicio;

import cl.rvasquez.banco.dao.ClienteDAO;
import cl.rvasquez.banco.dao.CuentaDAO;
import cl.rvasquez.banco.dao.impl.ClienteDAOImpl;
import cl.rvasquez.banco.dao.impl.CuentaDAOImpl;
import cl.rvasquez.banco.excepcion.ClienteNoExisteException;
import cl.rvasquez.banco.modelo.Cliente;
import cl.rvasquez.banco.modelo.Cuenta;
import cl.rvasquez.banco.modelo.CuentaCorriente;
import cl.rvasquez.banco.util.ConexionBD;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests de las reglas que viven en {@link CuentaService} más allá del
 * pass-through al DAO. Convención: una clase de test por production class.
 */
class CuentaServiceTest {

    private final ClienteDAO clienteDAO = new ClienteDAOImpl();
    private final CuentaDAO cuentaDAO = new CuentaDAOImpl();
    private final CuentaService cuentaService = new CuentaService(cuentaDAO, clienteDAO);

    @BeforeEach
    void resetearBD() throws SQLException {
        try (Connection c = ConexionBD.obtenerConexion();
             Statement s = c.createStatement()) {
            s.execute("TRUNCATE TABLE transaccion, cuenta, cliente RESTART IDENTITY CASCADE");
        }
    }

    @Test
    void crearCuentaConClienteInexistenteLanzaClienteNoExisteException() throws SQLException {
        Long clienteIdInexistente = 99_999L;
        Cuenta cuenta = new CuentaCorriente(
                null, "CTA-X", new BigDecimal("1000.0000"), clienteIdInexistente, null, true);

        ClienteNoExisteException ex = assertThrows(ClienteNoExisteException.class,
                () -> cuentaService.crear(cuenta));
        assertTrue(ex.getMessage().contains(String.valueOf(clienteIdInexistente)),
                "el mensaje debe identificar el cliente faltante");

        // No debe haber INSERT: la validación atajó antes de tocar BD.
        assertEquals(0, contarFilas("cuenta"));
    }

    @Test
    void crearCuentaConClienteValidoFunciona() throws SQLException {
        Cliente cliente = clienteDAO.insertar(
                new Cliente(null, "33.333.333-3", "Con", "Cuenta", "con@example.cl", null, null));

        Cuenta creada = cuentaService.crear(new CuentaCorriente(
                null, "CTA-OK", new BigDecimal("5000.0000"), cliente.getId(), null, true));

        assertNotNull(creada.getId());
        assertNotNull(creada.getFechaApertura());
        assertEquals(1, contarFilas("cuenta"));
    }

    private int contarFilas(String tabla) throws SQLException {
        try (Connection c = ConexionBD.obtenerConexion();
             Statement s = c.createStatement();
             var rs = s.executeQuery("SELECT count(*) FROM " + tabla)) {
            rs.next();
            return rs.getInt(1);
        }
    }
}
