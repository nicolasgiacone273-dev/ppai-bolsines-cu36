package ar.edu.utn.frc.dsi.bolsines.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entidad del Modelo de Dominio: Comisión Médica (CM).
 * El código es el que conocen los dispositivos GPS Tracker (ej.: CM-CBA-01).
 */
@Entity
@Table(name = "comision_medica")
public class ComisionMedica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String codigo;

    @Column(nullable = false)
    private String nombre;

    private String direccion;
    private String email;
    private String telefono;

    protected ComisionMedica() {
        // requerido por JPA
    }

    public ComisionMedica(String codigo, String nombre, String direccion, String email, String telefono) {
        this.codigo = codigo;
        this.nombre = nombre;
        this.direccion = direccion;
        this.email = email;
        this.telefono = telefono;
    }

    /** Dos instancias representan la misma CM si tienen el mismo código. */
    public boolean esLaMisma(ComisionMedica otra) {
        return otra != null && codigo.equalsIgnoreCase(otra.getCodigo());
    }

    public Long getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDireccion() {
        return direccion;
    }

    public String getEmail() {
        return email;
    }

    public String getTelefono() {
        return telefono;
    }
}
