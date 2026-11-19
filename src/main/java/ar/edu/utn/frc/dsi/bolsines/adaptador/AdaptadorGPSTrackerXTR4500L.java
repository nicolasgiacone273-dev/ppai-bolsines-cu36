package ar.edu.utn.frc.dsi.bolsines.adaptador;

import ar.edu.utn.frc.dsi.bolsines.entity.GPSTracker;
import ar.edu.utn.frc.dsi.bolsines.gpstracker.ErrorServicioGPSTrackerException;
import ar.edu.utn.frc.dsi.bolsines.gpstracker.ServicioGPSTrackerXTR4500L;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

/**
 * PATRÓN ADAPTER - rol ADAPTADOR (Adapter) para el modelo XTR-4500L.
 * <p>
 * Envuelve a {@link ServicioGPSTrackerXTR4500L} (Adaptable) y traduce:
 * <ul>
 *   <li>la llamada: obtenerUbicacion(...) → getBolsinLocation(apiKey, numeroBolsin, codigoCMOrigen)</li>
 *   <li>la respuesta: JSON en String con una lista "items" → {@link UbicacionBolsin}</li>
 *   <li>los errores: {@link ErrorServicioGPSTrackerException} → {@link UbicacionNoDisponibleException}</li>
 * </ul>
 */
@Component
public class AdaptadorGPSTrackerXTR4500L implements IAdaptadorGPSTracker {

    private final ServicioGPSTrackerXTR4500L servicioXTR4500L;
    private final String apiKey;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AdaptadorGPSTrackerXTR4500L(ServicioGPSTrackerXTR4500L servicioXTR4500L,
                                       @Value("${gps.tracker.xtr-4500l.api-key}") String apiKey) {
        this.servicioXTR4500L = servicioXTR4500L;
        this.apiKey = apiKey;
    }

    @Override
    public String getModelo() {
        return GPSTracker.MODELO_XTR_4500L;
    }

    @Override
    public UbicacionBolsin obtenerUbicacion(int numeroBolsin, String codigoCMOrigen, String codigoCMDestino)
            throws UbicacionNoDisponibleException {
        // 1. Traducción de la llamada: este modelo pide la CM ORIGEN (la CM destino no se usa).
        String json;
        try {
            json = servicioXTR4500L.getBolsinLocation(apiKey, numeroBolsin, codigoCMOrigen);
        } catch (ErrorServicioGPSTrackerException e) {
            throw UbicacionNoDisponibleException.porErrorDelServicio(getModelo(), e);
        }
        // 2. Traducción de la respuesta: se recorre la lista "items" del JSON.
        return interpretarJson(json, numeroBolsin);
    }

    private UbicacionBolsin interpretarJson(String json, int numeroBolsin) throws UbicacionNoDisponibleException {
        try {
            JsonNode items = objectMapper.readTree(json).path("items");
            for (JsonNode item : items) {
                if (item.path("numeroBolsin").asInt() == numeroBolsin) {
                    return new UbicacionBolsin(
                            item.path("numeroBolsin").asInt(),
                            item.path("latitud").asDouble(),
                            item.path("longitud").asDouble(),
                            LocalDateTime.parse(item.path("fechaHoraUltimaActualizacion").asText()));
                }
            }
        } catch (IOException | DateTimeParseException e) {
            throw UbicacionNoDisponibleException.porFormatoInvalido(getModelo());
        }
        throw UbicacionNoDisponibleException.porBolsinAusente(getModelo(), numeroBolsin);
    }
}
