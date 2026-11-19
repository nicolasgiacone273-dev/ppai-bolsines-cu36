package ar.edu.utn.frc.dsi.bolsines.dto;

import java.util.List;

/**
 * Lo que la pantalla necesita para el paso 6 del CU 36: la CM del usuario, los bolsines
 * ubicados (mapa), los que no se pudieron ubicar (A3), las CM destino para filtrar y,
 * si corresponde, un mensaje (A1, A2).
 *
 * @param tipoMensaje "info", "advertencia" o null si no hay mensaje.
 */
public record ResultadoSeguimientoDTO(String cmUsuarioCodigo,
                                      String cmUsuarioNombre,
                                      boolean hayBolsinesEnviados,
                                      List<BolsinUbicadoDTO> bolsinesUbicados,
                                      List<BolsinSinUbicacionDTO> bolsinesSinUbicacion,
                                      List<OpcionCMDTO> cmsDestino,
                                      String mensaje,
                                      String tipoMensaje) {
}
