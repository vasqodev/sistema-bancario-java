package cl.rvasquez.banco;

import cl.rvasquez.banco.dao.ClienteDAO;
import cl.rvasquez.banco.dao.CuentaDAO;
import cl.rvasquez.banco.dao.TransaccionDAO;
import cl.rvasquez.banco.dao.impl.ClienteDAOImpl;
import cl.rvasquez.banco.dao.impl.CuentaDAOImpl;
import cl.rvasquez.banco.dao.impl.TransaccionDAOImpl;
import cl.rvasquez.banco.modelo.Cliente;
import cl.rvasquez.banco.modelo.Cuenta;
import cl.rvasquez.banco.modelo.Transaccion;

import java.sql.SQLException;
import java.util.List;

/**
 * Smoke test de la capa DAO — criterio de aceptación de la Fase 1.
 *
 * Lista clientes, sus cuentas (mostrando el interés calculado polimórficamente
 * por cada subclase) y todas las transacciones registradas. Si esto corre y
 * muestra los datos semilla del schema, la capa de datos está lista para que
 * el servicio de transferencias (Fase 2) la use.
 *
 * No es parte del flujo de la app real. Ejecutar con:
 *   mvn -q compile exec:java -Dexec.mainClass=cl.rvasquez.banco.DemoDAO
 */
public class DemoDAO {

    public static void main(String[] args) throws SQLException {
        ClienteDAO clienteDAO = new ClienteDAOImpl();
        CuentaDAO cuentaDAO = new CuentaDAOImpl();
        TransaccionDAO transaccionDAO = new TransaccionDAOImpl();

        System.out.println("═══ CLIENTES Y SUS CUENTAS ═══");
        List<Cliente> clientes = clienteDAO.listar();
        for (Cliente c : clientes) {
            System.out.println(c);
            List<Cuenta> cuentas = cuentaDAO.buscarPorCliente(c.getId());
            for (Cuenta cuenta : cuentas) {
                // calcularInteres() es polimórfico: cada subclase aplica su tasa.
                System.out.println("    " + cuenta + "  interés mensual=" + cuenta.calcularInteres());
            }
        }

        System.out.println();
        System.out.println("═══ TRANSACCIONES REGISTRADAS ═══");
        List<Transaccion> transacciones = transaccionDAO.listar();
        for (Transaccion t : transacciones) {
            System.out.println(t);
        }

        System.out.println();
        System.out.println("DemoDAO OK");
    }
}
