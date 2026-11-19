package ar.edu.utn.frc.dsi.bolsines.boundary;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.logging.Logger;

/**
 * Interfaz con el actor secundario "Servidor de Correo".
 * <p>
 * El RNF 5 pide usar la API de Outlook (Microsoft Graph), que requiere una cuenta corporativa.
 * Para el práctico el envío está SIMULADO: cada correo se guarda como archivo .eml (se abre con
 * Outlook, Thunderbird o el Bloc de notas) en la carpeta configurada y se registra en la consola.
 * Para un envío real sólo habría que cambiar el cuerpo de {@link #enviarCorreo}.
 */
@Component
public class InterfazCorreoElectronico {

    private static final Logger LOG = Logger.getLogger(InterfazCorreoElectronico.class.getName());
    private static final DateTimeFormatter FORMATO_ARCHIVO = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS");

    private final Path carpetaSalida;
    private final String remitente;

    public InterfazCorreoElectronico(@Value("${correo.carpeta-salida}") String carpetaSalida,
                                     @Value("${correo.remitente}") String remitente) {
        this.carpetaSalida = Path.of(carpetaSalida);
        this.remitente = remitente;
    }

    /**
     * Envía el correo y devuelve la ruta del archivo .eml generado.
     */
    public String enviarCorreo(String destinatario, String asunto, String cuerpo) {
        String contenido = "From: " + remitente + "\r\n"
                + "To: " + destinatario + "\r\n"
                + "Subject: " + codificarAsunto(asunto) + "\r\n"
                + "MIME-Version: 1.0\r\n"
                + "Content-Type: text/plain; charset=UTF-8\r\n"
                + "Content-Transfer-Encoding: 8bit\r\n"
                + "\r\n"
                + cuerpo.replace("\n", "\r\n");
        try {
            Files.createDirectories(carpetaSalida);
            Path archivo = carpetaSalida.resolve(LocalDateTime.now().format(FORMATO_ARCHIVO) + ".eml");
            Files.writeString(archivo, contenido, StandardCharsets.UTF_8);
            LOG.info(() -> "Correo enviado a " + destinatario + " | " + asunto + " | copia: " + archivo.toAbsolutePath());
            return archivo.toString();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo enviar el correo a " + destinatario, e);
        }
    }

    /** Los encabezados de un correo sólo admiten ASCII: el asunto con tildes se codifica (RFC 2047). */
    private String codificarAsunto(String asunto) {
        return "=?UTF-8?B?" + Base64.getEncoder().encodeToString(asunto.getBytes(StandardCharsets.UTF_8)) + "?=";
    }
}
