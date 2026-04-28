package cl.rvasquez.banco;

import cl.rvasquez.banco.util.ConexionBD;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Punto de entrada de la aplicación.
 *
 * En la Fase 0 esta clase solo verifica que la conexión a la base de datos
 * funcione. En fases posteriores se reemplazará por el arranque de la UI Swing.
 */
public class Main {

    public static void main(String[] args) {
        try (Connection conexion = ConexionBD.obtenerConexion();
             PreparedStatement ps = conexion.prepareStatement("SELECT 1");
             ResultSet rs = ps.executeQuery()) {

            if (rs.next() && rs.getInt(1) == 1) {
                System.out.println("Conexión OK");
            } else {
                System.err.println("Conexión establecida pero respuesta inesperada de SELECT 1");
                System.exit(1);
            }
        } catch (SQLException e) {
            System.err.println("Error de conexión: " + e.getMessage());
            System.exit(1);
        }
    }
}
