package cl.rvasquez.banco.modelo;

/**
 * Discriminador del tipo de cuenta. Los valores deben coincidir exactamente
 * con la restricción CHECK de la columna cuenta.tipo en el schema SQL.
 */
public enum TipoCuenta {
    CORRIENTE,
    AHORRO
}
