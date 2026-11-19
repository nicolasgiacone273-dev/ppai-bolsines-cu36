package ar.edu.utn.frc.dsi.bolsines.gpstracker;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * PATRÓN ADAPTER - rol ADAPTABLE (Adaptee).
 * <p>
 * Representa la API del GPS Tracker modelo XTR-4500L tal como la describe la Observación 1
 * del CU 36: el endpoint getBolsinLocation() recibe una API Key, el número de bolsín (entero)
 * y el código de la CM ORIGEN (String), y devuelve un JSON en formato cadena de caracteres
 * con los ítems: número de bolsín, latitud, longitud y fecha/hora de última actualización.
 * <p>
 * Su interfaz es incompatible con la que espera el Gestor; por eso se la envuelve con
 * {@code AdaptadorGPSTrackerXTR4500L}. Esta clase NO conoce al adaptador ni al gestor.
 */
@Component
public class ServicioGPSTrackerXTR4500L {

    private final ClienteHttpGPSTracker clienteHttp;
    private final String urlBase;

    public ServicioGPSTrackerXTR4500L(ClienteHttpGPSTracker clienteHttp,
                                      @Value("${gps.tracker.xtr-4500l.url}") String urlBase) {
        this.clienteHttp = clienteHttp;
        this.urlBase = urlBase;
    }

    /**
     * Ejemplo de respuesta:
     * {"items":[{"numeroBolsin":101,"latitud":-31.4201,"longitud":-64.1888,"fechaHoraUltimaActualizacion":"2026-03-07T12:30:00"}]}
     */
    public String getBolsinLocation(String apiKey, int numeroBolsin, String codigoComisionMedicaOrigen) {
        Map<String, Object> parametros = new LinkedHashMap<>();
        parametros.put("numeroBolsin", numeroBolsin);
        parametros.put("codigoComisionMedicaOrigen", codigoComisionMedicaOrigen);
        return clienteHttp.get(urlBase, "getBolsinLocation", apiKey, parametros);
    }
}
