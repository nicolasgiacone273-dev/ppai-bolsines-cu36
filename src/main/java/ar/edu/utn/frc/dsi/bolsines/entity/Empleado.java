package ar.edu.utn.frc.dsi.bolsines.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Entidad del Modelo de Dominio: Empleado asignado a una Comisión Médica, con un Rol.
 */
@Entity
@Table(name = "empleado")
public class Empleado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;
    private String apellido;
    private String email;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "comision_medica_id")
    private ComisionMedica asignadoA;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "rol_id")
    private Rol rol;

    protected Empleado() {
        // requerido por JPA
    }

    public Empleado(String nombre, String apellido, String email, ComisionMedica asignadoA, Rol rol) {
        this.nombre = nombre;
        this.apellido = apellido;
        this.email = email;
        this.asignadoA = asignadoA;
        this.rol = rol;
    }

    /** Método del Modelo de Dominio: ¿el empleado está asignado a la CM recibida? */
    public boolean esTuCM(ComisionMedica comisionMedica) {
        return asignadoA != null && asignadoA.esLaMisma(comisionMedica);
    }

    /** Método del Modelo de Dominio: CM a la que está asignado el empleado. */
    public ComisionMedica getCM() {
        return asignadoA;
    }

    /** Método del Modelo de Dominio: ¿el empleado es Gerente de Comisión Médica? */
    public boolean sosGCM() {
        return rol != null && rol.esGCM();
    }

    public boolean sosEncargadoDeBolsines() {
        return rol != null && rol.esEncargadoDeBolsines();
    }

    public String getNombreCompleto() {
        return nombre + " " + apellido;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public String getEmail() {
        return email;
    }

    public Rol getRol() {
        return rol;
    }
}
