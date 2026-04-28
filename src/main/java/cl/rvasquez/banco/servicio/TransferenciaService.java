package cl.rvasquez.banco.servicio;

import cl.rvasquez.banco.dao.CuentaDAO;
import cl.rvasquez.banco.dao.TransaccionDAO;
import cl.rvasquez.banco.excepcion.CuentaNoExisteException;
import cl.rvasquez.banco.modelo.Cuenta;
import cl.rvasquez.banco.modelo.EstadoTransaccion;
import cl.rvasquez.banco.modelo.Transaccion;
import cl.rvasquez.banco.util.ConexionBD;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Servicio que ejecuta transferencias entre cuentas — el caso de uso central
 * de la app. Es donde concurrencia y transaccionalidad confluyen.
 *
 * Concurrencia:
 *   Cada cuenta tiene un {@link ReentrantLock} en {@link #locks}, creado on
 *   demand vía {@code computeIfAbsent}. Antes de operar, el servicio toma
 *   ambos locks SIEMPRE en orden ascendente de id. Eso previene el deadlock
 *   clásico A→B vs B→A, porque dos hilos que intenten cuentas {1,2} en
 *   sentidos opuestos terminan compitiendo por el lock(1) primero.
 *
 * Transaccionalidad:
 *   Débito + crédito + INSERT en transaccion ocurren bajo la misma Connection
 *   con autoCommit=false. Cualquier excepción dispara rollback y propaga, así
 *   que la BD nunca queda con un débito sin su crédito (consistencia atómica).
 *
 * Auditoría: en esta fase solo se registra la transferencia EXITOSA. Los
 * intentos rechazados se cuentan en {@link #transferenciasRechazadas} pero no
 * se persisten — el enum {@code RECHAZADA} queda disponible para una fase
 * posterior si se decide guardar también los fracasos.
 */
public class TransferenciaService {

    private final CuentaDAO cuentaDAO;
    private final TransaccionDAO transaccionDAO;

    private final ConcurrentHashMap<Long, ReentrantLock> locks = new ConcurrentHashMap<>();

    private final AtomicInteger transferenciasExitosas   = new AtomicInteger();
    private final AtomicInteger transferenciasRechazadas = new AtomicInteger();

    public TransferenciaService(CuentaDAO cuentaDAO, TransaccionDAO transaccionDAO) {
        this.cuentaDAO = cuentaDAO;
        this.transaccionDAO = transaccionDAO;
    }

    /**
     * Transfiere {@code monto} desde {@code origenId} a {@code destinoId}.
     * Devuelve la {@link Transaccion} persistida (con id y fecha completados).
     *
     * @throws IllegalArgumentException     si origen y destino son la misma cuenta.
     * @throws CuentaNoExisteException      si alguna cuenta no existe en la BD.
     * @throws cl.rvasquez.banco.excepcion.SaldoInsuficienteException si la origen no tiene saldo.
     * @throws cl.rvasquez.banco.excepcion.CuentaInactivaException    si alguna cuenta está inactiva.
     * @throws cl.rvasquez.banco.excepcion.MontoInvalidoException     si el monto no es positivo.
     * @throws SQLException                  ante cualquier error de BD (la transacción ya fue revertida).
     */
    public Transaccion transferir(Long origenId, Long destinoId, BigDecimal monto) throws SQLException {
        if (origenId.equals(destinoId)) {
            throw new IllegalArgumentException(
                    "La cuenta origen y la cuenta destino deben ser distintas");
        }

        boolean origenPrimero = origenId.compareTo(destinoId) < 0;
        Long primerId  = origenPrimero ? origenId  : destinoId;
        Long segundoId = origenPrimero ? destinoId : origenId;

        ReentrantLock lockA = locks.computeIfAbsent(primerId,  k -> new ReentrantLock());
        ReentrantLock lockB = locks.computeIfAbsent(segundoId, k -> new ReentrantLock());

        lockA.lock();
        try {
            lockB.lock();
            try {
                return ejecutarTransferencia(origenId, destinoId, monto);
            } finally {
                lockB.unlock();
            }
        } finally {
            lockA.unlock();
        }
    }

    private Transaccion ejecutarTransferencia(Long origenId, Long destinoId, BigDecimal monto)
            throws SQLException {

        // Cargamos las cuentas con conexiones autónomas: como tenemos los locks de
        // ambas cuentas, ningún otro hilo puede mutarlas mientras leemos.
        Cuenta origen = cuentaDAO.buscarPorId(origenId)
                .orElseThrow(() -> new CuentaNoExisteException(
                        "Cuenta origen " + origenId + " no existe"));
        Cuenta destino = cuentaDAO.buscarPorId(destinoId)
                .orElseThrow(() -> new CuentaNoExisteException(
                        "Cuenta destino " + destinoId + " no existe"));

        try (Connection conn = ConexionBD.obtenerConexion()) {
            conn.setAutoCommit(false);
            try {
                // aplicarDebito valida saldo suficiente, monto > 0 y cuenta activa.
                origen.aplicarDebito(monto);
                destino.aplicarCredito(monto);

                cuentaDAO.actualizar(origen,  conn);
                cuentaDAO.actualizar(destino, conn);

                Transaccion transaccion = new Transaccion(
                        null, origenId, destinoId, monto, null, EstadoTransaccion.EXITOSA);
                transaccionDAO.insertar(transaccion, conn);

                conn.commit();
                transferenciasExitosas.incrementAndGet();
                return transaccion;
            } catch (RuntimeException | SQLException e) {
                // Rollback "best effort": si rollback falla, perdemos la causa secundaria
                // pero conservamos la original — que es la que necesita el caller.
                try { conn.rollback(); } catch (SQLException ignored) { }
                transferenciasRechazadas.incrementAndGet();
                throw e;
            }
        }
    }

    public int getTransferenciasExitosas()   { return transferenciasExitosas.get(); }
    public int getTransferenciasRechazadas() { return transferenciasRechazadas.get(); }
}
