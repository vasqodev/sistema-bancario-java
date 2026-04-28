package cl.rvasquez.banco.modelo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Registro de auditoría de una transferencia entre cuentas.
 *
 * Las transferencias son el caso de uso central donde concurrencia y
 * transaccionalidad confluyen (ver TransferenciaService en Fase 2). Cada
 * intento — exitoso o rechazado — queda persistido aquí con su estado.
 *
 * id y fecha pueden ser null al construir; el DAO los completa tras INSERT.
 */
public class Transaccion {

    private Long id;
    private Long cuentaOrigenId;
    private Long cuentaDestinoId;
    private BigDecimal monto;
    private LocalDateTime fecha;
    private EstadoTransaccion estado;

    public Transaccion(Long id, Long cuentaOrigenId, Long cuentaDestinoId,
                       BigDecimal monto, LocalDateTime fecha, EstadoTransaccion estado) {
        this.id = id;
        this.fecha = fecha;
        setCuentaOrigenId(cuentaOrigenId);
        setCuentaDestinoId(cuentaDestinoId);
        setMonto(monto);
        setEstado(estado);
        // Espejo en código del CHECK (cuenta_origen_id <> cuenta_destino_id) del schema.
        if (cuentaOrigenId.equals(cuentaDestinoId)) {
            throw new IllegalArgumentException(
                    "cuentaOrigenId y cuentaDestinoId no pueden ser iguales");
        }
    }

    public Long getId() { return id; }
    public Long getCuentaOrigenId() { return cuentaOrigenId; }
    public Long getCuentaDestinoId() { return cuentaDestinoId; }
    public BigDecimal getMonto() { return monto; }
    public LocalDateTime getFecha() { return fecha; }
    public EstadoTransaccion getEstado() { return estado; }

    public void setId(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("id debe ser un Long positivo");
        }
        this.id = id;
    }

    public void setCuentaOrigenId(Long cuentaOrigenId) {
        if (cuentaOrigenId == null || cuentaOrigenId <= 0) {
            throw new IllegalArgumentException("cuentaOrigenId debe ser un Long positivo");
        }
        this.cuentaOrigenId = cuentaOrigenId;
    }

    public void setCuentaDestinoId(Long cuentaDestinoId) {
        if (cuentaDestinoId == null || cuentaDestinoId <= 0) {
            throw new IllegalArgumentException("cuentaDestinoId debe ser un Long positivo");
        }
        this.cuentaDestinoId = cuentaDestinoId;
    }

    public void setMonto(BigDecimal monto) {
        if (monto == null || monto.signum() <= 0) {
            throw new IllegalArgumentException("monto debe ser mayor que cero");
        }
        this.monto = monto;
    }

    public void setFecha(LocalDateTime fecha) {
        this.fecha = fecha;
    }

    public void setEstado(EstadoTransaccion estado) {
        if (estado == null) {
            throw new IllegalArgumentException("estado no puede ser nulo");
        }
        this.estado = estado;
    }

    @Override
    public String toString() {
        return "Transaccion{id=" + id +
                ", origen=" + cuentaOrigenId +
                " → destino=" + cuentaDestinoId +
                ", monto=" + monto +
                ", estado=" + estado +
                ", fecha=" + fecha +
                "}";
    }
}
