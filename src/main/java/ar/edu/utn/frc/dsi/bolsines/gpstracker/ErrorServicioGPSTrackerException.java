package ar.edu.utn.frc.dsi.bolsines.gpstracker;

/**
 * Error devuelto por la API de un GPS Tracker (API Key inválida, bolsín inexistente,
 * código de comisión que no coincide, servicio caído, etc.).
 */
public class ErrorServicioGPSTrackerException extends RuntimeException {

    /** Código HTTP devuelto por el servicio; 0 si no se pudo conectar. */
    private final int codigoHttp;

    public ErrorServicioGPSTrackerException(int codigoHttp, String mensaje) {
        super(mensaje);
        this.codigoHttp = codigoHttp;
    }

    public ErrorServicioGPSTrackerException(String mensaje, Throwable causa) {
        super(mensaje, causa);
        this.codigoHttp = 0;
    }

    public int getCodigoHttp() {
        return codigoHttp;
    }

    public boolean esFallaDeConexion() {
        return codigoHttp == 0;
    }
}
