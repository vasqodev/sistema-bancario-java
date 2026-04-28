package cl.rvasquez.banco.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Utilidad central para obtener conexiones a la base de datos.
 *
 * Diseño: NO mantiene una conexión singleton ni un pool. Cada llamada a
 * {@link #obtenerConexion()} abre una conexión nueva, que el llamador
 * debe cerrar (idealmente con try-with-resources). Esto encaja con la
 * estrategia transaccional acordada: el servicio que orquesta una
 * transferencia abre una conexión, la pasa a los DAOs involucrados, y
 * controla commit/rollback en un único punto.
 *
 * Las propiedades se leen de application.properties; cualquier valor
 * puede sobreescribirse vía variable de entorno (ver constantes ENV_*).
 */
public final class ConexionBD {

    private static final String ARCHIVO_PROPIEDADES = "application.properties";

    // Las variables de entorno tienen prioridad sobre los valores del .properties.
    private static final String ENV_HOST     = "POSTGRES_HOST";
    private static final String ENV_PORT     = "POSTGRES_PORT";
    private static final String ENV_DB       = "POSTGRES_DB";
    private static final String ENV_USER     = "POSTGRES_USER";
    private static final String ENV_PASSWORD = "POSTGRES_PASSWORD";

    private static final Properties PROPIEDADES = cargarPropiedades();

    private ConexionBD() {
        // Clase utilitaria: no se instancia.
    }

    public static Connection obtenerConexion() throws SQLException {
        String host     = resolver("db.host",     ENV_HOST);
        String port     = resolver("db.port",     ENV_PORT);
        String nombre   = resolver("db.name",     ENV_DB);
        String usuario  = resolver("db.user",     ENV_USER);
        String password = resolver("db.password", ENV_PASSWORD);

        String url = "jdbc:postgresql://" + host + ":" + port + "/" + nombre;
        return DriverManager.getConnection(url, usuario, password);
    }

    private static Properties cargarPropiedades() {
        Properties props = new Properties();
        try (InputStream in = ConexionBD.class.getClassLoader()
                .getResourceAsStream(ARCHIVO_PROPIEDADES)) {
            if (in == null) {
                throw new IllegalStateException(
                        "No se encontró " + ARCHIVO_PROPIEDADES + " en el classpath");
            }
            props.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Error leyendo " + ARCHIVO_PROPIEDADES, e);
        }
        return props;
    }

    private static String resolver(String clavePropiedad, String claveEnv) {
        String valorEnv = System.getenv(claveEnv);
        if (valorEnv != null && !valorEnv.isBlank()) {
            return valorEnv;
        }
        return PROPIEDADES.getProperty(clavePropiedad);
    }
}
