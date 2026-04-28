package cl.rvasquez.banco.dao.impl;

import cl.rvasquez.banco.dao.ClienteDAO;
import cl.rvasquez.banco.modelo.Cliente;
import cl.rvasquez.banco.util.ConexionBD;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementación JDBC pura de {@link ClienteDAO}.
 *
 * Convenciones aplicadas en todo el archivo:
 *  - Las queries van como constantes private static final al inicio (auditables
 *    de un vistazo, sin string-mongering en los métodos).
 *  - Toda query usa PreparedStatement (jamás Statement plano): previene
 *    SQL injection y deja el driver cachear el plan de ejecución.
 *  - Recursos JDBC (Connection, PreparedStatement, ResultSet) se cierran con
 *    try-with-resources.
 *  - Los métodos no atrapan SQLException; la propagan para que la capa
 *    superior (servicio o vista) decida cómo reportarla al usuario.
 */
public class ClienteDAOImpl implements ClienteDAO {

    private static final String SQL_INSERTAR = """
            INSERT INTO cliente (rut, nombre, apellido, email, telefono)
            VALUES (?, ?, ?, ?, ?)
            RETURNING id, fecha_creacion
            """;

    private static final String SQL_ACTUALIZAR = """
            UPDATE cliente
               SET rut = ?, nombre = ?, apellido = ?, email = ?, telefono = ?
             WHERE id = ?
            """;

    private static final String SQL_ELIMINAR = "DELETE FROM cliente WHERE id = ?";

    private static final String SQL_BUSCAR_POR_ID = """
            SELECT id, rut, nombre, apellido, email, telefono, fecha_creacion
              FROM cliente
             WHERE id = ?
            """;

    private static final String SQL_LISTAR = """
            SELECT id, rut, nombre, apellido, email, telefono, fecha_creacion
              FROM cliente
             ORDER BY id
            """;

    @Override
    public Cliente insertar(Cliente cliente) throws SQLException {
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERTAR)) {
            ps.setString(1, cliente.getRut());
            ps.setString(2, cliente.getNombre());
            ps.setString(3, cliente.getApellido());
            ps.setString(4, cliente.getEmail());
            ps.setString(5, cliente.getTelefono());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    cliente.setId(rs.getLong("id"));
                    cliente.setFechaCreacion(rs.getTimestamp("fecha_creacion").toLocalDateTime());
                }
            }
            return cliente;
        }
    }

    @Override
    public Cliente actualizar(Cliente cliente) throws SQLException {
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_ACTUALIZAR)) {
            ps.setString(1, cliente.getRut());
            ps.setString(2, cliente.getNombre());
            ps.setString(3, cliente.getApellido());
            ps.setString(4, cliente.getEmail());
            ps.setString(5, cliente.getTelefono());
            ps.setLong(6, cliente.getId());
            ps.executeUpdate();
            return cliente;
        }
    }

    @Override
    public boolean eliminar(Long id) throws SQLException {
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_ELIMINAR)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public Optional<Cliente> buscarPorId(Long id) throws SQLException {
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_BUSCAR_POR_ID)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapearFila(rs)) : Optional.empty();
            }
        }
    }

    @Override
    public List<Cliente> listar() throws SQLException {
        List<Cliente> resultado = new ArrayList<>();
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_LISTAR);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                resultado.add(mapearFila(rs));
            }
        }
        return resultado;
    }

    private Cliente mapearFila(ResultSet rs) throws SQLException {
        Timestamp ts = rs.getTimestamp("fecha_creacion");
        return new Cliente(
                rs.getLong("id"),
                rs.getString("rut"),
                rs.getString("nombre"),
                rs.getString("apellido"),
                rs.getString("email"),
                rs.getString("telefono"),
                ts != null ? ts.toLocalDateTime() : null
        );
    }
}
