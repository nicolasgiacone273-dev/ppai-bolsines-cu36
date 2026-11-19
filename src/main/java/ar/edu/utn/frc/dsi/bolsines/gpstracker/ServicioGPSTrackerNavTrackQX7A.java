package ar.edu.utn.frc.dsi.bolsines.gpstracker;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * PATRÓN ADAPTER - rol ADAPTABLE (Adaptee).
 * <p>
 * API del GPS Tracker modelo NavTrack QX-7A: el endpoint retrieveTrackingData() recibe una
 * API Key, el número de bolsín (entero) y el código de la CM DESTINO (String), y devuelve un
 * único String con los ítems separados por comas: número de bolsín, latitud, longitud y
 * fecha/hora del último reporte.
 */
@Component
public class ServicioGPSTrackerNavTrackQX7A {

    private final ClienteHttpGPSTracker clienteHttp;
    private final String urlBase;

    public ServicioGPSTrackerNavTrackQX7A(ClienteHttpGPSTracker clienteHttp,
                                          @Value("${gps.tracker.navtrack-qx-7a.url}") String urlBase) {
        this.clienteHttp = clienteHttp;
        this.urlBase = urlBase;
    }

    /** Ejemplo de respuesta: 101,-31.4201,-64.1888,2026-03-07T12:30 */
    public String retrieveTrackingData(String apiKey, int numeroBolsin, String codigoComisionMedicaDestino) {
        Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("numeroBolsin", numeroBolsin);
        parametros.put("codigoComisionMedicaDestino", codigoComisionMedicaDestino);
        return clienteHttp.get(urlBase, "retrieveTrackingData", apiKey, parametros);
    }
}
