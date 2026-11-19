package ar.edu.utn.frc.dsi.bolsines.adaptador;

import java.time.LocalDateTime;

/**
 * Formato ÚNICO en el que el Gestor recibe la ubicación de un bolsín, sin importar qué
 * modelo de GPS Tracker la informó: número de bolsín, ubicación actual (latitud y longitud)
 * y fecha y hora de la última actualización de posición (Observación 1 del CU 36).
 */
public class UbicacionBolsin {

    private final int numeroBolsin;
    private final double latitud;
    private final double longitud;
    private final LocalDateTime fechaHoraUltimaActualizacion;

    public UbicacionBolsin(int numeroBolsin, double latitud, double longitud, LocalDateTime fechaHoraUltimaActualizacion) {
        this.numeroBolsin = numeroBolsin;
        this.latitud = latitud;
        this.longitud = longitud;
        this.fechaHoraUltimaActualizacion = fechaHoraUltimaActualizacion;
    }

    public int getNumeroBolsin() {
        return numeroBolsin;
    }

    public double getLatitud() {
        return latitud;
    }

    public double getLongitud() {
        return longitud;
    }

    public LocalDateTime getFechaHoraUltimaActualizacion() {
        return fechaHoraUltimaActualizacion;
    }

    @Override
    public String toString() {
        return "UbicacionBolsin{numeroBolsin=" + numeroBolsin + ", latitud=" + latitud + ", longitud=" + longitud
                + ", fechaHoraUltimaActualizacion=" + fechaHoraUltimaActualizacion + '}';
    }
}
