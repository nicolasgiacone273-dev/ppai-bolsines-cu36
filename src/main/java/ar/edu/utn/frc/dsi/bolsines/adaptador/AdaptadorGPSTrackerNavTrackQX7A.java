package ar.edu.utn.frc.dsi.bolsines.adaptador;

import ar.edu.utn.frc.dsi.bolsines.entity.GPSTracker;
import ar.edu.utn.frc.dsi.bolsines.gpstracker.ErrorServicioGPSTrackerException;
import ar.edu.utn.frc.dsi.bolsines.gpstracker.ServicioGPSTrackerNavTrackQX7A;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

/**
 * PATRÓN ADAPTER - rol ADAPTADOR (Adapter) para el modelo NavTrack QX-7A.
 * <p>
 * Envuelve a {@link ServicioGPSTrackerNavTrackQX7A} (Adaptable) y traduce:
 * <ul>
 *   <li>la llamada: obtenerUbicacion(...) → retrieveTrackingData(apiKey, numeroBolsin, codigoCMDestino)</li>
 *   <li>la respuesta: un único String "nro,latitud,longitud,fechaHora" → {@link UbicacionBolsin}</li>
 *   <li>los errores → {@link UbicacionNoDisponibleException}</li>
 * </ul>
 */
@Component
public class AdaptadorGPSTrackerNavTrackQX7A implements IAdaptadorGPSTracker {

    /** Cada ítem del String tiene 4 campos: número, latitud, longitud y fecha/hora. */
    private static final int CAMPOS_POR_ITEM = 4;

    private final ServicioGPSTrackerNavTrackQX7A servicioNavTrackQX7A;
    private final String apiKey;

    public AdaptadorGPSTrackerNavTrackQX7A(ServicioGPSTrackerNavTrackQX7A servicioNavTrackQX7A,
                                           @Value("${gps.tracker.navtrack-qx-7a.api-key}") String apiKey) {
        this.servicioNavTrackQX7A = servicioNavTrackQX7A;
        this.apiKey = apiKey;
    }

    @Override
    public String getModelo() {
        return GPSTracker.MODELO_NAVTRACK_QX_7A;
    }

    @Override
    public UbicacionBolsin obtenerUbicacion(int numeroBolsin, String codigoCMOrigen, String codigoCMDestino)
            throws UbicacionNoDisponibleException {
        // 1. Traducción de la llamada: este modelo pide la CM DESTINO (la CM origen no se usa).
        String datos;
        try {
            datos = servicioNavTrackQX7A.retrieveTrackingData(apiKey, numeroBolsin, codigoCMDestino);
        } catch (ErrorServicioGPSTrackerException e) {
            throw UbicacionNoDisponibleException.porErrorDelServicio(getModelo(), e);
        }
        // 2. Traducción de la respuesta: se separa el String por comas, de a 4 campos por ítem.
        return interpretarTexto(datos, numeroBolsin);
    }

    private UbicacionBolsin interpretarTexto(String datos, int numeroBolsin) throws UbicacionNoDisponibleException {
        String[] campos = datos == null ? new String[0] : datos.trim().split(",");
        if (campos.length == 0 || campos.length % CAMPOS_POR_ITEM != 0) {
            throw UbicacionNoDisponibleException.porFormatoInvalido(getModelo());
        }
        try {
            for (int i = 0; i < campos.length; i += CAMPOS_POR_ITEM) {
                int numero = Integer.parseInt(campos[i].trim());
                if (numero == numeroBolsin) {
                    return new UbicacionBolsin(
                            numero,
                            Double.parseDouble(campos[i + 1].trim()),
                            Double.parseDouble(campos[i + 2].trim()),
                            LocalDateTime.parse(campos[i + 3].trim()));
                }
            }
        } catch (NumberFormatException | DateTimeParseException e) {
            throw UbicacionNoDisponibleException.porFormatoInvalido(getModelo());
        }
        throw UbicacionNoDisponibleException.porBolsinAusente(getModelo(), numeroBolsin);
    }
}
