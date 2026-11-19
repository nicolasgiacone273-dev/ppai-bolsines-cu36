package ar.edu.utn.frc.dsi.bolsines.boundary;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Interfaz con el servicio de mapas (boundary). Encapsula todo lo relativo al mapa: la API key de
 * Google Maps vive acá y no en la Pantalla, porque sólo el mapa la usa (mayor cohesión).
 * <p>
 * Igual que la Pantalla, tiene dos partes: esta clase, que entrega la configuración guardada en
 * application.properties, y {@code static/js/mapa.js}, que dibuja el mapa con Google Maps (si hay
 * API key) o con Leaflet + OpenStreetMap.
 */
@RestController
@RequestMapping("/api/mapa")
public class InterfazMapa {

    private final String googleMapsApiKey;

    public InterfazMapa(@Value("${google.maps.api-key:}") String googleMapsApiKey) {
        this.googleMapsApiKey = googleMapsApiKey;
    }

    /** Qué proveedor de mapas usar: Google Maps si hay API key configurada; si no, Leaflet + OpenStreetMap. */
    @GetMapping("/configuracion")
    public Map<String, String> obtenerConfiguracionMapa() {
        boolean hayKey = googleMapsApiKey != null && !googleMapsApiKey.isBlank();
        return Map.of(
                "proveedor", hayKey ? "google" : "leaflet",
                "apiKey", hayKey ? googleMapsApiKey : "");
    }
}
