package ar.edu.utn.frc.dsi.bolsines.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entidad del Modelo de Dominio: Rol del empleado dentro de la comisión médica.
 */
@Entity
@Table(name = "rol")
public class Rol {

    public static final String GERENTE_COMISION_MEDICA = "Gerente de Comisión Médica";
    public static final String ENCARGADO_BOLSINES = "Encargado de Bolsines";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nombre;

    private String descripcion;

    protected Rol() {
        // requerido por JPA
    }

    public Rol(String nombre, String descripcion) {
        this.nombre = nombre;
        this.descripcion = descripcion;
    }

    /** Método del Modelo de Dominio: indica si el rol es Gerente de Comisión Médica. */
    public boolean esGCM() {
        return GERENTE_COMISION_MEDICA.equalsIgnoreCase(nombre);
    }

    public boolean esEncargadoDeBolsines() {
        return ENCARGADO_BOLSINES.equalsIgnoreCase(nombre);
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
