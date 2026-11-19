package ar.edu.utn.frc.dsi.bolsines.gpstracker;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * PATRÓN ADAPTER - rol ADAPTABLE (Adaptee).
 * <p>
 * API del GPS Tracker modelo GeoPulse MTR-900: el endpoint fetchCargoPositions() recibe una
 * API Key y el número de bolsín (entero), y devuelve una MATRIZ donde cada fila tiene:
 * número de bolsín, latitud, longitud y fecha/hora de la última actualización de posición.
 */
@Component
public class ServicioGPSTrackerGeoPulseMTR900 {

    private final ClienteHttpGPSTracker clienteHttp;
    private final String urlBase;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ServicioGPSTrackerGeoPulseMTR900(ClienteHttpGPSTracker clienteHttp,
                                            @Value("${gps.tracker.geopulse-mtr-900.url}") String urlBase) {
        this.clienteHttp = clienteHttp;
        this.urlBase = urlBase;
    }

    /** Ejemplo de respuesta: [[101, -31.4201, -64.1888, "2026-03-07T12:30:00"]] */
    public Object[][] fetchCargoPositions(String apiKey, int numeroBolsin) {
        Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("numeroBolsin", numeroBolsin);
        String json = clienteHttp.get(urlBase, "fetchCargoPositions", apiKey, parametros);
        try {
            return objectMapper.readValue(json, Object[][].class);
        } catch (JsonProcessingException e) {
            throw new ErrorServicioGPSTrackerException(200, "La respuesta del GeoPulse MTR-900 no es una matriz válida");
        }
    }
}
