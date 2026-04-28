package cl.rvasquez.banco.modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Cuenta corriente: tasa de interés mensual = 0%. La pieza polimórfica clave
 * es que {@link #calcularInteres()} siempre devuelve cero, sin que el código
 * llamador (servicio o vista) tenga que distinguir el tipo con un if/switch.
 */
public class CuentaCorriente extends Cuenta {

    private static final BigDecimal TASA_MENSUAL = BigDecimal.ZERO;

    public CuentaCorriente(Long id, String numeroCuenta, BigDecimal saldo,
                           Long clienteId, LocalDateTime fechaApertura, boolean activa) {
        super(id, numeroCuenta, saldo, clienteId, fechaApertura, activa);
    }

    @Override
    public TipoCuenta getTipo() {
        return TipoCuenta.CORRIENTE;
    }

    @Override
    public BigDecimal calcularInteres() {
        return getSaldo().multiply(TASA_MENSUAL);
    }
}
