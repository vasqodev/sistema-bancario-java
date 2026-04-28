package cl.rvasquez.banco.modelo;

/**
 * Estado final de una transferencia. Los valores deben coincidir exactamente
 * con la restricción CHECK de la columna transaccion.estado en el schema SQL.
 */
public enum EstadoTransaccion {
    EXITOSA,
    RECHAZADA
}
