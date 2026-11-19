package ar.edu.utn.frc.dsi.bolsines.adaptador;

/**
 * PATRÓN ADAPTER - rol OBJETIVO (Target).
 * <p>
 * Es la única interfaz que conoce el Cliente ({@code GestorConsultarSeguimientoBolsines})
 * para pedir la ubicación de un bolsín. Cada modelo de GPS Tracker tiene una API distinta
 * (nombre del endpoint, parámetros y formato de respuesta), así que cada uno se envuelve en un
 * adaptador que implementa esta interfaz.
 * <p>
 * La firma incluye los dos códigos de CM porque XTR-4500L pide la CM origen y NavTrack QX-7A
 * pide la CM destino; cada adaptador usa sólo lo que su API necesita.
 */
public interface IAdaptadorGPSTracker {

    /** Modelo de GPS Tracker que sabe adaptar (ej.: "XTR-4500L"). */
    String getModelo();

    /**
     * Obtiene la ubicación actual del bolsín.
     *
     * @throws UbicacionNoDisponibleException si el GPS Tracker no pudo informarla (flujo A3).
     */
    UbicacionBolsin obtenerUbicacion(int numeroBolsin, String codigoCMOrigen, String codigoCMDestino)
            throws UbicacionNoDisponibleException;
}
