package cl.rvasquez.banco.modelo;

import java.time.LocalDateTime;

/**
 * Entidad de dominio que representa a un cliente del banco.
 *
 * Encapsulamiento: atributos privados, acceso vía getters/setters.
 * Las setters validan inputs externos y lanzan IllegalArgumentException si
 * violan las invariantes básicas (no nulos, no en blanco). El constructor
 * delega en las setters para que la validación viva en un único lugar.
 *
 * id y fechaCreacion pueden quedar en null al instanciar un Cliente nuevo;
 * el DAO los completa tras INSERT (RETURNING id, fecha_creacion).
 */
public class Cliente {

    private Long id;
    private String rut;
    private String nombre;
    private String apellido;
    private String email;
    private String telefono;
    private LocalDateTime fechaCreacion;

    public Cliente(Long id, String rut, String nombre, String apellido,
                   String email, String telefono, LocalDateTime fechaCreacion) {
        this.id = id;
        this.fechaCreacion = fechaCreacion;
        setRut(rut);
        setNombre(nombre);
        setApellido(apellido);
        setEmail(email);
        setTelefono(telefono);
    }

    public Long getId() { return id; }
    public String getRut() { return rut; }
    public String getNombre() { return nombre; }
    public String getApellido() { return apellido; }
    public String getEmail() { return email; }
    public String getTelefono() { return telefono; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }

    public void setId(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("id debe ser un Long positivo");
        }
        this.id = id;
    }

    public void setRut(String rut) {
        if (rut == null || rut.isBlank()) {
            throw new IllegalArgumentException("rut no puede ser nulo ni vacío");
        }
        this.rut = rut.trim();
    }

    public void setNombre(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new IllegalArgumentException("nombre no puede ser nulo ni vacío");
        }
        this.nombre = nombre.trim();
    }

    public void setApellido(String apellido) {
        if (apellido == null || apellido.isBlank()) {
            throw new IllegalArgumentException("apellido no puede ser nulo ni vacío");
        }
        this.apellido = apellido.trim();
    }

    public void setEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("email no puede ser nulo ni vacío");
        }
        this.email = email.trim();
    }

    public void setTelefono(String telefono) {
        // Teléfono es opcional según schema (columna nullable).
        this.telefono = (telefono == null || telefono.isBlank()) ? null : telefono.trim();
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    @Override
    public String toString() {
        return "Cliente{id=" + id +
                ", rut=" + rut +
                ", nombre=" + nombre + " " + apellido +
                ", email=" + email +
                "}";
    }
}
