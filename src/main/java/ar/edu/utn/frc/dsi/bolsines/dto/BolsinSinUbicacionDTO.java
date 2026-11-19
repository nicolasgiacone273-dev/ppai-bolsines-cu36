package ar.edu.utn.frc.dsi.bolsines.dto;

/** Flujo alternativo A3: bolsín cuyo GPS Tracker no pudo informar la ubicación. */
public record BolsinSinUbicacionDTO(int numeroBolsin,
                                    Integer numeroPrecinto,
                                    String cmDestinoNombre,
                                    String modeloGPSTracker,
                                    String motivo) {
}
