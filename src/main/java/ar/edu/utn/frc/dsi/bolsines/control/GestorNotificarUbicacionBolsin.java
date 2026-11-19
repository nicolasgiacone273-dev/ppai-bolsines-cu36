package ar.edu.utn.frc.dsi.bolsines.control;

import ar.edu.utn.frc.dsi.bolsines.boundary.InterfazCorreoElectronico;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Gestor del CU 30 "Notificar ubicación de bolsín", INCLUIDO por el CU 36 en su paso 11.
 * <p>
 * Nota: el ERS dice "se incluye al Caso de Uso 31", pero en el listado de funcionalidades
 * el CU 31 es "Registrar Revisión de Documentación"; el de notificar ubicación es el CU 30.
 */
@Component
public class GestorNotificarUbicacionBolsin {

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final InterfazCorreoElectronico interfazCorreo;

    public GestorNotificarUbicacionBolsin(InterfazCorreoElectronico interfazCorreo) {
        this.interfazCorreo = interfazCorreo;
    }

    /**
     * Informa número de bolsín, fecha y hora de última actualización y coordenadas.
     *
     * @return ruta de la copia del correo enviado.
     */
    public String notificarUbicacionBolsin(String emailDestinatario, String nombreCMDestino, int numeroBolsin,
                                           LocalDateTime fechaHoraUltimaActualizacion,
                                           double latitud, double longitud) {
        String coordenadas = String.format(Locale.ROOT, "%.6f, %.6f", latitud, longitud);
        String asunto = "Ubicación del bolsín N° " + numeroBolsin;
        String cuerpo = "Gerente de " + nombreCMDestino + ":\n\n"
                + "Le informamos la última ubicación registrada del bolsín que se dirige a su comisión médica.\n\n"
                + "Número de bolsín: " + numeroBolsin + "\n"
                + "Última actualización: " + fechaHoraUltimaActualizacion.format(FORMATO) + "\n"
                + "Coordenadas (latitud, longitud): " + coordenadas + "\n"
                + "Ver en el mapa: https://www.google.com/maps?q=" + coordenadas.replace(" ", "") + "\n\n"
                + "Este mensaje fue generado automáticamente por el Sistema de Gestión de Bolsines.";
        return interfazCorreo.enviarCorreo(emailDestinatario, asunto, cuerpo);
    }
}
