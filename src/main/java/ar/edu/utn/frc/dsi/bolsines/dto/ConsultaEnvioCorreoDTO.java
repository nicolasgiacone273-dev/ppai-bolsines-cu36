package ar.edu.utn.frc.dsi.bolsines.dto;

/** Paso 8 del CU 36: pregunta si se desea enviar el correo al GCM de la CM destino. */
public record ConsultaEnvioCorreoDTO(int numeroBolsin,
                                     Integer numeroPrecinto,
                                     String cmDestinoNombre,
                                     String fechaHoraUltimaActualizacion,
                                     double latitud,
                                     double longitud,
                                     String pregunta) {
}
