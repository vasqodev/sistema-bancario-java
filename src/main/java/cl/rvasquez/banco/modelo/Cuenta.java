package cl.rvasquez.banco.modelo;

import cl.rvasquez.banco.excepcion.CuentaInactivaException;
import cl.rvasquez.banco.excepcion.MontoInvalidoException;
import cl.rvasquez.banco.excepcion.SaldoInsuficienteException;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidad de dominio abstracta que representa una cuenta bancaria.
 *
 * El tipo concreto (CORRIENTE / AHORRO) se modela por subclase: cada una
 * implementa {@link #getTipo()} y {@link #calcularInteres()} con su propia
 * tasa. Esto da polimorfismo real: el servicio invoca calcularInteres() sin
 * conocer la subclase.
 *
 * El campo {@code saldo} es la única excepción al patrón "atributo privado
 * + setter público": se asigna solo por constructor (hidratación desde DAO)
 * y se muta exclusivamente a través de {@link #aplicarDebito(BigDecimal)} y
 * {@link #aplicarCredito(BigDecimal)}, que protegen la invariante saldo >= 0
 * y la regla "no operar sobre cuentas inactivas".
 */
public abstract class Cuenta {

    private Long id;
    private String numeroCuenta;
    private BigDecimal saldo;
    private Long clienteId;
    private LocalDateTime fechaApertura;
    private boolean activa;

    protected Cuenta(Long id, String numeroCuenta, BigDecimal saldo,
                     Long clienteId, LocalDateTime fechaApertura, boolean activa) {
        this.id = id;
        this.fechaApertura = fechaApertura;
        this.activa = activa;
        setNumeroCuenta(numeroCuenta);
        setClienteId(clienteId);
        // saldo no tiene setter público; validamos directo aquí.
        if (saldo == null || saldo.signum() < 0) {
            throw new IllegalArgumentException("saldo no puede ser nulo ni negativo");
        }
        this.saldo = saldo;
    }

    public abstract TipoCuenta getTipo();

    /**
     * Devuelve el monto de interés correspondiente al saldo actual.
     * No muta la cuenta — la decisión de aplicarlo (vía {@link #aplicarCredito})
     * la toma el servicio.
     */
    public abstract BigDecimal calcularInteres();

    // ── Operaciones de dominio sobre el saldo ───────────────────────────────

    public void aplicarDebito(BigDecimal monto) {
        validarMontoPositivo(monto);
        verificarActiva();
        if (saldo.compareTo(monto) < 0) {
            throw new SaldoInsuficienteException(
                    "Saldo insuficiente en " + numeroCuenta +
                    ": disponible " + saldo + ", requerido " + monto);
        }
        this.saldo = this.saldo.subtract(monto);
    }

    public void aplicarCredito(BigDecimal monto) {
        validarMontoPositivo(monto);
        verificarActiva();
        this.saldo = this.saldo.add(monto);
    }

    private void validarMontoPositivo(BigDecimal monto) {
        if (monto == null || monto.signum() <= 0) {
            throw new MontoInvalidoException("El monto debe ser mayor que cero");
        }
    }

    private void verificarActiva() {
        if (!activa) {
            throw new CuentaInactivaException(
                    "La cuenta " + numeroCuenta + " está inactiva");
        }
    }

    // ── Getters / setters ───────────────────────────────────────────────────

    public Long getId() { return id; }
    public String getNumeroCuenta() { return numeroCuenta; }
    public BigDecimal getSaldo() { return saldo; }
    public Long getClienteId() { return clienteId; }
    public LocalDateTime getFechaApertura() { return fechaApertura; }
    public boolean isActiva() { return activa; }

    public void setId(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("id debe ser un Long positivo");
        }
        this.id = id;
    }

    public void setNumeroCuenta(String numeroCuenta) {
        if (numeroCuenta == null || numeroCuenta.isBlank()) {
            throw new IllegalArgumentException("numeroCuenta no puede ser nulo ni vacío");
        }
        this.numeroCuenta = numeroCuenta.trim();
    }

    public void setClienteId(Long clienteId) {
        if (clienteId == null || clienteId <= 0) {
            throw new IllegalArgumentException("clienteId debe ser un Long positivo");
        }
        this.clienteId = clienteId;
    }

    public void setFechaApertura(LocalDateTime fechaApertura) {
        this.fechaApertura = fechaApertura;
    }

    public void setActiva(boolean activa) {
        this.activa = activa;
    }

    @Override
    public String toString() {
        return getTipo() + "{id=" + id +
                ", numero=" + numeroCuenta +
                ", saldo=" + saldo +
                ", clienteId=" + clienteId +
                ", activa=" + activa +
                "}";
    }
}
