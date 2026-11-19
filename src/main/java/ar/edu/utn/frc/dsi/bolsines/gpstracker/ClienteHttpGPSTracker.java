package ar.edu.utn.frc.dsi.bolsines.gpstracker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Cliente HTTP compartido por los servicios de los GPS Tracker.
 * Hace un GET con el header "X-API-Key" (así lo exige la API de la cátedra)
 * y devuelve el cuerpo de la respuesta tal como llega.
 */
@Component
public class ClienteHttpGPSTracker {

    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(TIMEOUT)
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    public String get(String urlBase, String endpoint, String apiKey, Map<String, Object> parametros) {
        String url = urlBase + "/" + endpoint + "?" + armarQuery(parametros);
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(TIMEOUT)
                .header("X-API-Key", apiKey == null ? "" : apiKey)
                .GET()
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() != 200) {
                throw new ErrorServicioGPSTrackerException(response.statusCode(), extraerMensaje(response.body()));
            }
            return response.body();
        } catch (IOException e) {
            throw new ErrorServicioGPSTrackerException("No se pudo conectar con " + urlBase, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ErrorServicioGPSTrackerException("Se interrumpió la consulta a " + urlBase, e);
        }
    }

    private String armarQuery(Map<String, Object> parametros) {
        return new LinkedHashMap<>(parametros).entrySet().stream()
                .map(p -> p.getKey() + "=" + URLEncoder.encode(String.valueOf(p.getValue()), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
    }

    /** La API de la cátedra responde los errores como {"error": ..., "mensaje": ...}. */
    private String extraerMensaje(String cuerpo) {
        try {
            JsonNode json = objectMapper.readTree(cuerpo);
            if (json != null && json.hasNonNull("mensaje")) {
                return json.get("mensaje").asText();
            }
        } catch (IOException | RuntimeException ignorada) {
            // el cuerpo no era JSON: se devuelve tal cual
        }
        return cuerpo;
    }
}
