package ar.edu.utn.frc.dsi.bolsines.dto;

/**
 * Resultado de los pasos 9 a 11 (o del flujo A5 si el EB no confirma el envío).
 *
 * @param finDelCasoDeUso true cuando el CU terminó (correo enviado).
 */
public record ResultadoNotificacionDTO(boolean correoEnviado,
                                       boolean finDelCasoDeUso,
                                       String mensaje,
                                       String destinatario,
                                       String copiaGuardadaEn) {
}
