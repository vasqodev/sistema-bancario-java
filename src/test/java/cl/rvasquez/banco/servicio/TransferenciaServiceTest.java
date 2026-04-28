package cl.rvasquez.banco.servicio;

import cl.rvasquez.banco.dao.ClienteDAO;
import cl.rvasquez.banco.dao.CuentaDAO;
import cl.rvasquez.banco.dao.TransaccionDAO;
import cl.rvasquez.banco.dao.impl.ClienteDAOImpl;
import cl.rvasquez.banco.dao.impl.CuentaDAOImpl;
import cl.rvasquez.banco.dao.impl.TransaccionDAOImpl;
import cl.rvasquez.banco.excepcion.SaldoInsuficienteException;
import cl.rvasquez.banco.modelo.Cliente;
import cl.rvasquez.banco.modelo.Cuenta;
import cl.rvasquez.banco.modelo.CuentaCorriente;
import cl.rvasquez.banco.modelo.Transaccion;
import cl.rvasquez.banco.util.ConexionBD;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests de integración del caso de uso central — transferencias.
 *
 * Aislamiento: corre contra la BD `banco_test`, configurada vía
 * {@code <environmentVariables><POSTGRES_DB>banco_test</POSTGRES_DB></...>}
 * en el surefire del pom. Cada test trunca y siembra sus propios datos en
 * {@link #resetearBD()}.
 */
class TransferenciaServiceTest {

    private final ClienteDAO clienteDAO = new ClienteDAOImpl();
    private final CuentaDAO cuentaDAO = new CuentaDAOImpl();
    private final TransaccionDAO transaccionDAO = new TransaccionDAOImpl();
    private final TransferenciaService service = new TransferenciaService(cuentaDAO, transaccionDAO);

    @BeforeEach
    void resetearBD() throws SQLException {
        // RESTART IDENTITY → reinicia BIGSERIAL para que los ids de cada test sean reproducibles.
        // CASCADE → no se requiere por dependencia, pero protege ante futuros FK adicionales.
        try (Connection c = ConexionBD.obtenerConexion();
             Statement s = c.createStatement()) {
            s.execute("TRUNCATE TABLE transaccion, cuenta, cliente RESTART IDENTITY CASCADE");
        }
    }

    // ─── (a) Transferencia simple debita y acredita ─────────────────────────

    @Test
    void transferenciaSimpleDebitaYAcredita() throws SQLException {
        Cliente cliente = clienteDAO.insertar(nuevoCliente("11.111.111-1"));
        Cuenta origen  = cuentaDAO.insertar(nuevaCorriente("CTA-A", "100000.0000", cliente.getId()));
        Cuenta destino = cuentaDAO.insertar(nuevaCorriente("CTA-B",  "50000.0000", cliente.getId()));

        Transaccion resultado = service.transferir(origen.getId(), destino.getId(), new BigDecimal("30000.0000"));

        assertNotNull(resultado.getId(), "la transacción debe quedar persistida con id");
        assertNotNull(resultado.getFecha(), "la transacción debe traer fecha desde RETURNING");

        Cuenta origenFinal  = cuentaDAO.buscarPorId(origen.getId()).orElseThrow();
        Cuenta destinoFinal = cuentaDAO.buscarPorId(destino.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("70000.0000").compareTo(origenFinal.getSaldo()));
        assertEquals(0, new BigDecimal("80000.0000").compareTo(destinoFinal.getSaldo()));

        assertEquals(1, transaccionDAO.listar().size());
        assertEquals(1, service.getTransferenciasExitosas());
        assertEquals(0, service.getTransferenciasRechazadas());
    }

    // ─── (b) Saldo insuficiente lanza excepción y rollback ──────────────────

    @Test
    void saldoInsuficienteLanzaExcepcionYHaceRollback() throws SQLException {
        Cliente cliente = clienteDAO.insertar(nuevoCliente("22.222.222-2"));
        Cuenta origen  = cuentaDAO.insertar(nuevaCorriente("CTA-A",  "1000.0000", cliente.getId()));
        Cuenta destino = cuentaDAO.insertar(nuevaCorriente("CTA-B", "50000.0000", cliente.getId()));

        assertThrows(SaldoInsuficienteException.class,
                () -> service.transferir(origen.getId(), destino.getId(), new BigDecimal("5000.0000")));

        // Rollback: los saldos en BD deben quedar tal como antes.
        Cuenta origenFinal  = cuentaDAO.buscarPorId(origen.getId()).orElseThrow();
        Cuenta destinoFinal = cuentaDAO.buscarPorId(destino.getId()).orElseThrow();
        assertEquals(0, new BigDecimal( "1000.0000").compareTo(origenFinal.getSaldo()));
        assertEquals(0, new BigDecimal("50000.0000").compareTo(destinoFinal.getSaldo()));

        // No insertamos auditoría de rechazos en esta fase.
        assertEquals(0, transaccionDAO.listar().size());
        assertEquals(0, service.getTransferenciasExitosas());
        assertEquals(1, service.getTransferenciasRechazadas());
    }

    // ─── (c) N transferencias concurrentes preservan el saldo total ─────────

    @Test
    void transferenciasConcurrentesPreservanSaldoTotal() throws Exception {
        Cliente cliente = clienteDAO.insertar(nuevoCliente("33.333.333-3"));
        Cuenta a = cuentaDAO.insertar(nuevaCorriente("CTA-A", "100000.0000", cliente.getId()));
        Cuenta b = cuentaDAO.insertar(nuevaCorriente("CTA-B", "100000.0000", cliente.getId()));

        BigDecimal totalAntes = a.getSaldo().add(b.getSaldo());
        int n = 50;
        BigDecimal monto = new BigDecimal("1000.0000");
        AtomicInteger exitosas   = new AtomicInteger();
        AtomicInteger rechazadas = new AtomicInteger();

        ExecutorService pool = Executors.newFixedThreadPool(10);
        CountDownLatch fin = new CountDownLatch(n);
        for (int i = 0; i < n; i++) {
            final boolean aHaciaB = (i % 2 == 0);
            pool.submit(() -> {
                try {
                    Long origenId  = aHaciaB ? a.getId() : b.getId();
                    Long destinoId = aHaciaB ? b.getId() : a.getId();
                    service.transferir(origenId, destinoId, monto);
                    exitosas.incrementAndGet();
                } catch (Exception e) {
                    rechazadas.incrementAndGet();
                } finally {
                    fin.countDown();
                }
            });
        }
        assertTrue(fin.await(15, TimeUnit.SECONDS), "los 50 hilos deben terminar antes de 15s");
        pool.shutdown();

        Cuenta aFinal = cuentaDAO.buscarPorId(a.getId()).orElseThrow();
        Cuenta bFinal = cuentaDAO.buscarPorId(b.getId()).orElseThrow();
        BigDecimal totalDespues = aFinal.getSaldo().add(bFinal.getSaldo());

        assertEquals(0, totalAntes.compareTo(totalDespues),
                "el dinero total debe conservarse: ningún débito sin su crédito");
        assertEquals(n, exitosas.get() + rechazadas.get());
        assertEquals(exitosas.get(), service.getTransferenciasExitosas());
        assertEquals(rechazadas.get(), service.getTransferenciasRechazadas());
    }

    // ─── (d) Auto-transferencia rechazada ───────────────────────────────────

    @Test
    void autoTransferenciaEsRechazada() throws SQLException {
        Cliente cliente = clienteDAO.insertar(nuevoCliente("44.444.444-4"));
        Cuenta cuenta = cuentaDAO.insertar(nuevaCorriente("CTA-A", "100000.0000", cliente.getId()));

        assertThrows(IllegalArgumentException.class,
                () -> service.transferir(cuenta.getId(), cuenta.getId(), new BigDecimal("1000.0000")));

        // El servicio debe rechazar antes de tocar BD.
        Cuenta cuentaFinal = cuentaDAO.buscarPorId(cuenta.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("100000.0000").compareTo(cuentaFinal.getSaldo()));
        assertEquals(0, transaccionDAO.listar().size());
    }

    // ─── (e) Deadlock test: A→B y B→A en paralelo ────────────────────────────

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void transferenciasOpuestasNoSeBloquean() throws Exception {
        Cliente cliente = clienteDAO.insertar(nuevoCliente("55.555.555-5"));
        Cuenta a = cuentaDAO.insertar(nuevaCorriente("CTA-A", "100000.0000", cliente.getId()));
        Cuenta b = cuentaDAO.insertar(nuevaCorriente("CTA-B", "100000.0000", cliente.getId()));

        int iter = 30;
        BigDecimal monto = new BigDecimal("100.0000");

        // Sincronización de arranque: ambos hilos esperan en `start` para arrancar
        // a la vez y maximizar la ventana en que se cruzan los pedidos de lock.
        CountDownLatch listos  = new CountDownLatch(2);
        CountDownLatch arranca = new CountDownLatch(1);

        ExecutorService pool = Executors.newFixedThreadPool(2);
        Future<?> hiloAB = pool.submit(() -> {
            listos.countDown();
            arranca.await();
            for (int i = 0; i < iter; i++) {
                service.transferir(a.getId(), b.getId(), monto);
            }
            return null;
        });
        Future<?> hiloBA = pool.submit(() -> {
            listos.countDown();
            arranca.await();
            for (int i = 0; i < iter; i++) {
                service.transferir(b.getId(), a.getId(), monto);
            }
            return null;
        });

        listos.await();
        arranca.countDown();
        // Si las firmas de lock divergen, esto colgará y @Timeout fallará el test.
        hiloAB.get();
        hiloBA.get();
        pool.shutdown();

        // Cada par AB+BA es net-zero, así que el saldo final debe ser igual al inicial.
        Cuenta aFinal = cuentaDAO.buscarPorId(a.getId()).orElseThrow();
        Cuenta bFinal = cuentaDAO.buscarPorId(b.getId()).orElseThrow();
        assertEquals(0, new BigDecimal("100000.0000").compareTo(aFinal.getSaldo()));
        assertEquals(0, new BigDecimal("100000.0000").compareTo(bFinal.getSaldo()));

        List<Transaccion> historial = transaccionDAO.listar();
        assertEquals(iter * 2, historial.size());
    }

    // ─── Helpers ────────────────────────────────────────────────────────────

    private Cliente nuevoCliente(String rut) {
        return new Cliente(null, rut, "Test", "User", "test@example.cl", null, null);
    }

    private CuentaCorriente nuevaCorriente(String numero, String saldo, Long clienteId) {
        return new CuentaCorriente(null, numero, new BigDecimal(saldo), clienteId, null, true);
    }
}
