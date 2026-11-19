package ar.edu.utn.frc.dsi.bolsines.dto;

/** Un bolsín con ubicación informada por su GPS Tracker, listo para dibujarse en el mapa. */
public record BolsinUbicadoDTO(int numeroBolsin,
                               Integer numeroPrecinto,
                               String cmDestinoCodigo,
                               String cmDestinoNombre,
                               String modeloGPSTracker,
                               double latitud,
                               double longitud,
                               String fechaHoraUltimaActualizacion) {
}
