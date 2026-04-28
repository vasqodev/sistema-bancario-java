package cl.rvasquez.banco.modelo;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

/**
 * Cuenta de ahorro: tasa de interés mensual = 0,5%. Demuestra polimorfismo
 * frente a CuentaCorriente: misma firma calcularInteres(), distinto resultado.
 */
public class CuentaAhorro extends Cuenta {

    private static final BigDecimal TASA_MENSUAL = new BigDecimal("0.005");

    public CuentaAhorro(Long id, String numeroCuenta, BigDecimal saldo,
                        Long clienteId, LocalDateTime fechaApertura, boolean activa) {
        super(id, numeroCuenta, saldo, clienteId, fechaApertura, activa);
    }

    @Override
    public TipoCuenta getTipo() {
        return TipoCuenta.AHORRO;
    }

    @Override
    public BigDecimal calcularInteres() {
        // HALF_EVEN (banker's rounding) es el estándar en cálculos monetarios:
        // reduce sesgo acumulado al redondear muchos cálculos sucesivos.
        return getSaldo().multiply(TASA_MENSUAL).setScale(4, RoundingMode.HALF_EVEN);
    }
}
