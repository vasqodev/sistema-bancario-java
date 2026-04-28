package cl.rvasquez.banco.dao.impl;

import cl.rvasquez.banco.dao.CuentaDAO;
import cl.rvasquez.banco.modelo.Cuenta;
import cl.rvasquez.banco.modelo.CuentaAhorro;
import cl.rvasquez.banco.modelo.CuentaCorriente;
import cl.rvasquez.banco.modelo.TipoCuenta;
import cl.rvasquez.banco.util.ConexionBD;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementación JDBC pura de {@link CuentaDAO}.
 *
 * Polimorfismo a nivel de DAO: {@link #mapearFila(ResultSet)} actúa como
 * factory — lee la columna {@code tipo} y devuelve la subclase concreta
 * ({@link CuentaCorriente} o {@link CuentaAhorro}) sin que los métodos
 * de listado tengan que conocer la jerarquía.
 *
 * Sobrecarga transaccional en {@link #actualizar(Cuenta, Connection)}:
 * la versión autónoma abre/cierra su Connection y delega en la versión
 * transaccional, que usa la Connection externa y la deja abierta. Patrón
 * usado por TransferenciaService para encadenar débito + crédito + auditoría
 * en una única transacción JDBC.
 */
public class CuentaDAOImpl implements CuentaDAO {

    private static final String SQL_INSERTAR = """
            INSERT INTO cuenta (numero_cuenta, tipo, saldo, cliente_id, activa)
            VALUES (?, ?, ?, ?, ?)
            RETURNING id, fecha_apertura
            """;

    private static final String SQL_ACTUALIZAR = """
            UPDATE cuenta
               SET numero_cuenta = ?, tipo = ?, saldo = ?, cliente_id = ?, activa = ?
             WHERE id = ?
            """;

    private static final String SQL_ELIMINAR = "UPDATE cuenta SET activa = false WHERE id = ?";

    private static final String SQL_COLUMNAS =
            "id, numero_cuenta, tipo, saldo, cliente_id, fecha_apertura, activa";

    private static final String SQL_BUSCAR_POR_ID =
            "SELECT " + SQL_COLUMNAS + " FROM cuenta WHERE id = ?";

    private static final String SQL_LISTAR =
            "SELECT " + SQL_COLUMNAS + " FROM cuenta ORDER BY id";

    private static final String SQL_LISTAR_ACTIVAS =
            "SELECT " + SQL_COLUMNAS + " FROM cuenta WHERE activa = true ORDER BY id";

    private static final String SQL_BUSCAR_POR_CLIENTE =
            "SELECT " + SQL_COLUMNAS + " FROM cuenta WHERE cliente_id = ? ORDER BY id";

    private static final String SQL_BUSCAR_ACTIVAS_POR_CLIENTE =
            "SELECT " + SQL_COLUMNAS + " FROM cuenta WHERE cliente_id = ? AND activa = true ORDER BY id";

    @Override
    public Cuenta insertar(Cuenta cuenta) throws SQLException {
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_INSERTAR)) {
            ps.setString(1, cuenta.getNumeroCuenta());
            ps.setString(2, cuenta.getTipo().name());
            ps.setBigDecimal(3, cuenta.getSaldo());
            ps.setLong(4, cuenta.getClienteId());
            ps.setBoolean(5, cuenta.isActiva());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    cuenta.setId(rs.getLong("id"));
                    cuenta.setFechaApertura(rs.getTimestamp("fecha_apertura").toLocalDateTime());
                }
            }
            return cuenta;
        }
    }

    @Override
    public Cuenta actualizar(Cuenta cuenta) throws SQLException {
        try (Connection conn = ConexionBD.obtenerConexion()) {
            return actualizar(cuenta, conn);
        }
    }

    @Override
    public Cuenta actualizar(Cuenta cuenta, Connection conn) throws SQLException {
        // Variante transaccional: NO cerramos la conexión recibida.
        try (PreparedStatement ps = conn.prepareStatement(SQL_ACTUALIZAR)) {
            ps.setString(1, cuenta.getNumeroCuenta());
            ps.setString(2, cuenta.getTipo().name());
            ps.setBigDecimal(3, cuenta.getSaldo());
            ps.setLong(4, cuenta.getClienteId());
            ps.setBoolean(5, cuenta.isActiva());
            ps.setLong(6, cuenta.getId());
            ps.executeUpdate();
            return cuenta;
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
    public Optional<Cuenta> buscarPorId(Long id) throws SQLException {
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(SQL_BUSCAR_POR_ID)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapearFila(rs)) : Optional.empty();
            }
        }
    }

    @Override
    public List<Cuenta> listar() throws SQLException {
        return ejecutarListado(SQL_LISTAR, null);
    }

    @Override
    public List<Cuenta> listarActivas() throws SQLException {
        return ejecutarListado(SQL_LISTAR_ACTIVAS, null);
    }

    @Override
    public List<Cuenta> buscarPorCliente(Long clienteId) throws SQLException {
        return ejecutarListado(SQL_BUSCAR_POR_CLIENTE, clienteId);
    }

    @Override
    public List<Cuenta> buscarActivasPorCliente(Long clienteId) throws SQLException {
        return ejecutarListado(SQL_BUSCAR_ACTIVAS_POR_CLIENTE, clienteId);
    }

    /**
     * Helper privado: ejecuta cualquier SELECT que devuelva filas de cuenta.
     * Si {@code parametroOpcional} no es null, lo usa como primer parámetro.
     * Evita duplicar la maquinaria de Connection/PreparedStatement/ResultSet
     * en los cuatro métodos de listado.
     */
    private List<Cuenta> ejecutarListado(String sql, Long parametroOpcional) throws SQLException {
        List<Cuenta> resultado = new ArrayList<>();
        try (Connection conn = ConexionBD.obtenerConexion();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (parametroOpcional != null) {
                ps.setLong(1, parametroOpcional);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    resultado.add(mapearFila(rs));
                }
            }
        }
        return resultado;
    }

    /**
     * Factory polimórfico: instancia la subclase concreta según el discriminador
     * {@code tipo}. Si en el futuro aparece un nuevo tipo de cuenta, basta con
     * agregar un nuevo {@code case} en este switch (el compilador avisa porque
     * el switch sobre enum es exhaustivo en Java 21).
     */
    private Cuenta mapearFila(ResultSet rs) throws SQLException {
        Long id = rs.getLong("id");
        String numero = rs.getString("numero_cuenta");
        TipoCuenta tipo = TipoCuenta.valueOf(rs.getString("tipo"));
        java.math.BigDecimal saldo = rs.getBigDecimal("saldo");
        Long clienteId = rs.getLong("cliente_id");
        java.time.LocalDateTime fechaApertura = rs.getTimestamp("fecha_apertura").toLocalDateTime();
        boolean activa = rs.getBoolean("activa");

        return switch (tipo) {
            case CORRIENTE -> new CuentaCorriente(id, numero, saldo, clienteId, fechaApertura, activa);
            case AHORRO    -> new CuentaAhorro(id, numero, saldo, clienteId, fechaApertura, activa);
        };
    }
}
