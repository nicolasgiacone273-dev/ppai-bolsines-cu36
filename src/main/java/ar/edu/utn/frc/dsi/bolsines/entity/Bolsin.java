package ar.edu.utn.frc.dsi.bolsines.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad del Modelo de Dominio: Bolsín que transporta remitos entre comisiones médicas.
 * <p>
 * Para el CU 36 se agrega la asociación con su dispositivo {@link GPSTracker} (RNF 6).
 * Los remitos no se modelan aquí porque el CU 36 no los utiliza.
 */
@Entity
@Table(name = "bolsin")
public class Bolsin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Integer numeroBolsin;

    private Integer numeroPrecinto;

    private LocalDate fecha;

    /** Peso expresado en gramos. */
    private Double peso;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "cm_origen_id")
    private ComisionMedica origen;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "cm_destino_id")
    private ComisionMedica destino;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "bolsin_id")
    private List<CambioEstadoBolsin> cambioEstado = new ArrayList<>();

    @OneToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL, optional = false)
    @JoinColumn(name = "gps_tracker_id")
    private GPSTracker gpsTracker;

    protected Bolsin() {
        // requerido por JPA
    }

    public Bolsin(Integer numeroBolsin, Integer numeroPrecinto, LocalDate fecha, Double peso,
                  ComisionMedica origen, ComisionMedica destino, GPSTracker gpsTracker) {
        this.numeroBolsin = numeroBolsin;
        this.numeroPrecinto = numeroPrecinto;
        this.fecha = fecha;
        this.peso = peso;
        this.origen = origen;
        this.destino = destino;
        this.gpsTracker = gpsTracker;
    }

    // ------------------------------------------------------------------
    // Métodos usados por el CU 36
    // ------------------------------------------------------------------

    /** Método del Modelo de Dominio: ¿la CM origen del bolsín es la CM recibida? */
    public boolean esTuCMOrigen(ComisionMedica comisionMedica) {
        return origen != null && origen.esLaMisma(comisionMedica);
    }

    /** Método del Modelo de Dominio: ¿el estado actual del bolsín es Enviado? */
    public boolean sosEnviado() {
        for (CambioEstadoBolsin cambio : cambioEstado) {
            if (cambio.sosActual()) {
                return cambio.sosEnviado();
            }
        }
        return false;
    }

    /** Método del Modelo de Dominio. */
    public Integer getNumeroBolsin() {
        return numeroBolsin;
    }

    /** Método del Modelo de Dominio. */
    public Integer getNroPrecinto() {
        return numeroPrecinto;
    }

    /** Método del Modelo de Dominio. */
    public ComisionMedica obtenerCMDestino() {
        return destino;
    }

    public String obtenerCodigoCMOrigen() {
        return origen.getCodigo();
    }

    public String obtenerCodigoCMDestino() {
        return destino.getCodigo();
    }

    /** Devuelve el modelo del GPS Tracker que lleva el bolsín (se usa para elegir el adaptador). */
    public String conocerModeloGPSTracker() {
        return gpsTracker == null ? null : gpsTracker.getModelo();
    }

    public boolean tenesPrecinto(Integer precinto) {
        return precinto != null && precinto.equals(numeroPrecinto);
    }

    public boolean tenesCMDestino(String codigoCM) {
        return codigoCM != null && destino.getCodigo().equalsIgnoreCase(codigoCM);
    }

    /**
     * Método del Modelo de Dominio: registra un nuevo estado, cerrando el cambio de estado vigente.
     * (Lo usa la carga de datos iniciales; el CU 36 no cambia estados.)
     */
    public void crearCEBolsin(Estado nuevoEstado, LocalDateTime fechaHora, Empleado responsable) {
        for (CambioEstadoBolsin cambio : cambioEstado) {
            if (cambio.sosActual()) {
                cambio.setFechaHoraFin(fechaHora);
            }
        }
        cambioEstado.add(new CambioEstadoBolsin(fechaHora, nuevoEstado, responsable));
    }

    // ------------------------------------------------------------------
    // Getters
    // ------------------------------------------------------------------

    public Long getId() {
        return id;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public Double getPeso() {
        return peso;
    }

    public ComisionMedica getOrigen() {
        return origen;
    }

    public ComisionMedica getDestino() {
        return destino;
    }

    public List<CambioEstadoBolsin> getCambioEstado() {
        return cambioEstado;
    }

    public GPSTracker getGpsTracker() {
        return gpsTracker;
    }
}
