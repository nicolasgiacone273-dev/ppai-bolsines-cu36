package ar.edu.utn.frc.dsi.bolsines.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entidad del Modelo de Dominio: Estado (con su ámbito: Bolsin, Remito, Documentacion).
 */
@Entity
@Table(name = "estado")
public class Estado {

    public static final String AMBITO_BOLSIN = "Bolsin";

    public static final String CREADO = "Creado";
    public static final String CERRADO = "Cerrado";
    public static final String ENVIADO = "Enviado";
    public static final String RECIBIDO_EN_CM_DESTINO = "Recibido en CM destino";
    public static final String DE_BAJA = "De baja";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String ambito;

    @Column(nullable = false)
    private String nombre;

    private String descripcion;

    protected Estado() {
        // requerido por JPA
    }

    public Estado(String ambito, String nombre, String descripcion) {
        this.ambito = ambito;
        this.nombre = nombre;
        this.descripcion = descripcion;
    }

    /** Método del Modelo de Dominio. */
    public boolean esAmbitoBolsin() {
        return AMBITO_BOLSIN.equalsIgnoreCase(ambito);
    }

    /** Método del Modelo de Dominio. */
    public boolean esEnviado() {
        return esAmbitoBolsin() && ENVIADO.equalsIgnoreCase(nombre);
    }

    public Long getId() {
        return id;
    }

    public String getAmbito() {
        return ambito;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
