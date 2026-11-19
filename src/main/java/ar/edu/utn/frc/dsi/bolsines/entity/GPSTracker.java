package ar.edu.utn.frc.dsi.bolsines.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entidad AGREGADA al Modelo de Dominio para el diseño (no está en el modelo de la cátedra).
 * <p>
 * El RNF 6 del ERS dice que "cada bolsín debe contar con un dispositivo GPS Tracker".
 * Esta clase representa ese dispositivo y guarda su modelo (XTR-4500L, NavTrack QX-7A o
 * GeoPulse MTR-900), que es lo que el Gestor usa para elegir el adaptador correspondiente.
 */
@Entity
@Table(name = "gps_tracker")
public class GPSTracker {

    public static final String MODELO_XTR_4500L = "XTR-4500L";
    public static final String MODELO_NAVTRACK_QX_7A = "NavTrack QX-7A";
    public static final String MODELO_GEOPULSE_MTR_900 = "GeoPulse MTR-900";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String numeroSerie;

    @Column(nullable = false)
    private String modelo;

    protected GPSTracker() {
        // requerido por JPA
    }

    public GPSTracker(String numeroSerie, String modelo) {
        this.numeroSerie = numeroSerie;
        this.modelo = modelo;
    }

    public Long getId() {
        return id;
    }

    public String getNumeroSerie() {
        return numeroSerie;
    }

    public String getModelo() {
        return modelo;
    }
}
