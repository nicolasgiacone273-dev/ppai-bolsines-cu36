package ar.edu.utn.frc.dsi.bolsines.adaptador;

import ar.edu.utn.frc.dsi.bolsines.gpstracker.ErrorServicioGPSTrackerException;

/**
 * Flujo alternativo A3 del CU 36: "El GPS Tracker no pudo informar ubicación del bolsín".
 * Los adaptadores traducen cualquier error del Adaptable a esta excepción, de modo que el
 * Gestor maneja el problema siempre igual, sea cual sea el modelo de GPS.
 */
public class UbicacionNoDisponibleException extends Exception {

    public UbicacionNoDisponibleException(String motivo) {
        super(motivo);
    }

    public static UbicacionNoDisponibleException porErrorDelServicio(String modelo, ErrorServicioGPSTrackerException error) {
        if (error.esFallaDeConexion()) {
            return new UbicacionNoDisponibleException(
                    "No se pudo conectar con el servicio del GPS Tracker " + modelo + ".");
        }
        if (error.getCodigoHttp() == 401) {
            return new UbicacionNoDisponibleException(
                    "El GPS Tracker " + modelo + " rechazó la API Key configurada.");
        }
        return new UbicacionNoDisponibleException(
                "El GPS Tracker " + modelo + " no informó la ubicación. Respuesta del dispositivo: " + error.getMessage());
    }

    public static UbicacionNoDisponibleException porFormatoInvalido(String modelo) {
        return new UbicacionNoDisponibleException(
                "El GPS Tracker " + modelo + " devolvió datos con un formato que no se pudo interpretar.");
    }

    public static UbicacionNoDisponibleException porBolsinAusente(String modelo, int numeroBolsin) {
        return new UbicacionNoDisponibleException(
                "El GPS Tracker " + modelo + " no incluyó al bolsín " + numeroBolsin + " en su respuesta.");
    }
}
