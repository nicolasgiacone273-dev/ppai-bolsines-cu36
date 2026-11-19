package ar.edu.utn.frc.dsi.bolsines.adaptador;

import ar.edu.utn.frc.dsi.bolsines.entity.GPSTracker;
import ar.edu.utn.frc.dsi.bolsines.gpstracker.ErrorServicioGPSTrackerException;
import ar.edu.utn.frc.dsi.bolsines.gpstracker.ServicioGPSTrackerGeoPulseMTR900;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

/**
 * PATRÓN ADAPTER - rol ADAPTADOR (Adapter) para el modelo GeoPulse MTR-900.
 * <p>
 * Envuelve a {@link ServicioGPSTrackerGeoPulseMTR900} (Adaptable) y traduce:
 * <ul>
 *   <li>la llamada: obtenerUbicacion(...) → fetchCargoPositions(apiKey, numeroBolsin)
 *       (este modelo no recibe códigos de comisión médica)</li>
 *   <li>la respuesta: matriz Object[][] (una fila por bolsín) → {@link UbicacionBolsin}</li>
 *   <li>los errores → {@link UbicacionNoDisponibleException}</li>
 * </ul>
 */
@Component
public class AdaptadorGPSTrackerGeoPulseMTR900 implements IAdaptadorGPSTracker {

    private static final int COLUMNA_NUMERO = 0;
    private static final int COLUMNA_LATITUD = 1;
    private static final int COLUMNA_LONGITUD = 2;
    private static final int COLUMNA_FECHA_HORA = 3;

    private final ServicioGPSTrackerGeoPulseMTR900 servicioGeoPulseMTR900;
    private final String apiKey;

    public AdaptadorGPSTrackerGeoPulseMTR900(ServicioGPSTrackerGeoPulseMTR900 servicioGeoPulseMTR900,
                                             @Value("${gps.tracker.geopulse-mtr-900.api-key}") String apiKey) {
        this.servicioGeoPulseMTR900 = servicioGeoPulseMTR900;
        this.apiKey = apiKey;
    }

    @Override
    public String getModelo() {
        return GPSTracker.MODELO_GEOPULSE_MTR_900;
    }

    @Override
    public UbicacionBolsin obtenerUbicacion(int numeroBolsin, String codigoCMOrigen, String codigoCMDestino)
            throws UbicacionNoDisponibleException {
        // 1. Traducción de la llamada: sólo API Key y número de bolsín.
        Object[][] matriz;
        try {
            matriz = servicioGeoPulseMTR900.fetchCargoPositions(apiKey, numeroBolsin);
        } catch (ErrorServicioGPSTrackerException e) {
            throw UbicacionNoDisponibleException.porErrorDelServicio(getModelo(), e);
        }
        // 2. Traducción de la respuesta: se recorre la matriz fila por fila.
        return interpretarMatriz(matriz, numeroBolsin);
    }

    private UbicacionBolsin interpretarMatriz(Object[][] matriz, int numeroBolsin) throws UbicacionNoDisponibleException {
        if (matriz == null) {
            throw UbicacionNoDisponibleException.porFormatoInvalido(getModelo());
        }
        try {
            for (Object[] fila : matriz) {
                int numero = ((Number) fila[COLUMNA_NUMERO]).intValue();
                if (numero == numeroBolsin) {
                    return new UbicacionBolsin(
                            numero,
                            ((Number) fila[COLUMNA_LATITUD]).doubleValue(),
                            ((Number) fila[COLUMNA_LONGITUD]).doubleValue(),
                            LocalDateTime.parse(String.valueOf(fila[COLUMNA_FECHA_HORA])));
                }
            }
        } catch (ClassCastException | ArrayIndexOutOfBoundsException | NullPointerException | DateTimeParseException e) {
            throw UbicacionNoDisponibleException.porFormatoInvalido(getModelo());
        }
        throw UbicacionNoDisponibleException.porBolsinAusente(getModelo(), numeroBolsin);
    }
}
