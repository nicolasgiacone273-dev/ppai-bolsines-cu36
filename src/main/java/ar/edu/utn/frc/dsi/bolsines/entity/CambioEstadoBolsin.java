package ar.edu.utn.frc.dsi.bolsines.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Entidad del Modelo de Dominio: historial de estados del Bolsín, con el empleado responsable.
 */
@Entity
@Table(name = "cambio_estado_bolsin")
public class CambioEstadoBolsin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime fechaHoraInicio;
    private LocalDateTime fechaHoraFin;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "estado_id")
    private Estado estado;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "responsable_id")
    private Empleado responsableCE;

    protected CambioEstadoBolsin() {
        // requerido por JPA
    }

    public CambioEstadoBolsin(LocalDateTime fechaHoraInicio, Estado estado, Empleado responsableCE) {
        this.fechaHoraInicio = fechaHoraInicio;
        this.estado = estado;
        this.responsableCE = responsableCE;
    }

    /** Método del Modelo de Dominio: el cambio de estado vigente es el que no tiene fecha de fin. */
    public boolean sosActual() {
        return fechaHoraFin == null;
    }

    /** Método del Modelo de Dominio. */
    public boolean sosEnviado() {
        return estado != null && estado.esEnviado();
    }

    /** Método del Modelo de Dominio. */
    public void setFechaHoraFin(LocalDateTime fechaHoraFin) {
        this.fechaHoraFin = fechaHoraFin;
    }

    public Long getId() {
        return id;
    }

    public LocalDateTime getFechaHoraInicio() {
        return fechaHoraInicio;
    }

    public LocalDateTime getFechaHoraFin() {
        return fechaHoraFin;
    }

    public Estado getEstado() {
        return estado;
    }

    public Empleado getResponsableCE() {
        return responsableCE;
    }
}
