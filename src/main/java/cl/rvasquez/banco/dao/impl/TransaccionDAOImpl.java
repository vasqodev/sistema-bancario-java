package cl.rvasquez.banco.dao.impl;

import cl.rvasquez.banco.dao.TransaccionDAO;
import cl.rvasquez.banco.modelo.EstadoTransaccion;
import cl.rvasquez.banco.modelo.Transaccion;
import cl.rvasquez.banco.util.ConexionBD;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementación JDBC pura de {@link TransaccionDAO}.
 *
 * Sobrecarga transaccional en {@link #insertar(Transaccion, Connection)}:
 * permite que TransferenciaService registre el evento dentro de la misma
 * transacción JDBC en la que aplica débito y crédito. La versión autónoma
 * abre/cierra su propia Connection y delega en la transaccional.
 */
public class TransaccionDAOImpl implements TransaccionDAO {

    private static final String SQL_INSERTAR = """
            INSERT INTO transaccion (cuenta_origen_id, cuenta_destino_id, monto, estado)
            VALUES (?, ?, ?, ?)
            RETURNING id, fecha
            """;

    private static final String SQL_LISTAR_POR_CUENTA = """
            SELECT id, cuenta_origen_id, cuenta_destino_id, monto, fecha, estado
              FROM transaccion
             WHERE cuenta_origen_id = ? OR cuenta_destino_id = ?
             ORDER BY fecha DESC, id DESC
            """;

    private static final String SQL_LISTAR = """
            SELECT id, cuenta_origen_id, cuenta_destino_id, monto, fecha, estado
              FROM transaccion
             ORDER BY fecha DESC, id DESC
            """;

    @Override
    public Transaccion insertar(Transaccion transaccion) throws SQLException {
        try (Connection conn = ConexionBD.obtenerConexion()) {
            return insertar(transaccion, conn);
        }
    }

    @Override
    public Transaccion insertar(Transaccion transaccion, Connection conn) throws SQLException {
        // Variante transaccional: NO cerramos la conexión recibida.
        try (PreparedStatement ps = conn.prepareStatement(SQL_INSERTAR)) {
            ps.setLong(1, transaccion.getCuentaOrigenId());
            ps.setLong(2, transaccion.getCuentaDestinoId());
            ps.setBigDecimal(3, transaccion.getMonto());
            ps.setString(4, transaccion.getEstado().name());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    transaccion.setId(rs.getLong("id"));
                    transaccion.setFecha(rs.getTimestamp("fecha").toLocalDateTime());
                }
            }
            return transaccion;
        }
    }

    @Override
    public List<Transaccion> listarPorCuenta(Long cuentaId) throws SQLException {
        List<Transaccion> resultado = new ArrayList<>();
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_LISTAR_POR_CUENTA)) {
            ps.setLong(1, cuentaId);
            ps.setLong(2, cuentaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    resultado.add(mapearFila(rs));
                }
            }
        }
        return resultado;
    }

    @Override
    public List<Transaccion> listar() throws SQLException {
        List<Transaccion> resultado = new ArrayList<>();
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_LISTAR);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                resultado.add(mapearFila(rs));
            }
        }
        return resultado;
    }

    private Transaccion mapearFila(ResultSet rs) throws SQLException {
        return new Transaccion(
                rs.getLong("id"),
                rs.getLong("cuenta_origen_id"),
                rs.getLong("cuenta_destino_id"),
                rs.getBigDecimal("monto"),
                rs.getTimestamp("fecha").toLocalDateTime(),
                EstadoTransaccion.valueOf(rs.getString("estado"))
        );
    }
}
