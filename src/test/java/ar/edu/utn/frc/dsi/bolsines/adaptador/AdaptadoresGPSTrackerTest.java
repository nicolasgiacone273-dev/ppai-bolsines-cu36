package ar.edu.utn.frc.dsi.bolsines.adaptador;

import ar.edu.utn.frc.dsi.bolsines.gpstracker.ErrorServicioGPSTrackerException;
import ar.edu.utn.frc.dsi.bolsines.gpstracker.ServicioGPSTrackerGeoPulseMTR900;
import ar.edu.utn.frc.dsi.bolsines.gpstracker.ServicioGPSTrackerNavTrackQX7A;
import ar.edu.utn.frc.dsi.bolsines.gpstracker.ServicioGPSTrackerXTR4500L;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prueba que los tres adaptadores devuelven la MISMA estructura (UbicacionBolsin) a partir de
 * las tres respuestas distintas de cada API. Los Adaptables se reemplazan por versiones falsas
 * que devuelven exactamente lo que devuelve la API mock de la cátedra, sin usar la red.
 */
class AdaptadoresGPSTrackerTest {

    private static final LocalDateTime FECHA = LocalDateTime.of(2026, 3, 7, 12, 30, 0);

    @Test
    void xtr4500lInterpretaElJsonDeItems() throws Exception {
        ServicioGPSTrackerXTR4500L servicioFalso = new ServicioGPSTrackerXTR4500L(null, "sin-red") {
            @Override
            public String getBolsinLocation(String apiKey, int numeroBolsin, String codigoCMOrigen) {
                assertEquals("CM-CBA-01", codigoCMOrigen, "XTR-4500L debe recibir la CM ORIGEN");
                return "{\"items\":[{\"numeroBolsin\":101,\"latitud\":-31.4201,\"longitud\":-64.1888,"
                        + "\"fechaHoraUltimaActualizacion\":\"2026-03-07T12:30:00\"}]}";
            }
        };
        IAdaptadorGPSTracker adaptador = new AdaptadorGPSTrackerXTR4500L(servicioFalso, "key");

        UbicacionBolsin ubicacion = adaptador.obtenerUbicacion(101, "CM-CBA-01", "CM-ROS-02");

        verificar(ubicacion);
    }

    @Test
    void navTrackInterpretaElStringSeparadoPorComas() throws Exception {
        ServicioGPSTrackerNavTrackQX7A servicioFalso = new ServicioGPSTrackerNavTrackQX7A(null, "sin-red") {
            @Override
            public String retrieveTrackingData(String apiKey, int numeroBolsin, String codigoCMDestino) {
                assertEquals("CM-ROS-02", codigoCMDestino, "NavTrack QX-7A debe recibir la CM DESTINO");
                // La API mock arma la fecha con LocalDateTime.toString(): si los segundos son 0, no los incluye
                return "101,-31.4201,-64.1888,2026-03-07T12:30";
            }
        };
        IAdaptadorGPSTracker adaptador = new AdaptadorGPSTrackerNavTrackQX7A(servicioFalso, "key");

        UbicacionBolsin ubicacion = adaptador.obtenerUbicacion(101, "CM-CBA-01", "CM-ROS-02");

        verificar(ubicacion);
    }

    @Test
    void geoPulseInterpretaLaMatriz() throws Exception {
        ServicioGPSTrackerGeoPulseMTR900 servicioFalso = new ServicioGPSTrackerGeoPulseMTR900(null, "sin-red") {
            @Override
            public Object[][] fetchCargoPositions(String apiKey, int numeroBolsin) {
                return new Object[][]{{101, -31.4201, -64.1888, "2026-03-07T12:30:00"}};
            }
        };
        IAdaptadorGPSTracker adaptador = new AdaptadorGPSTrackerGeoPulseMTR900(servicioFalso, "key");

        UbicacionBolsin ubicacion = adaptador.obtenerUbicacion(101, "CM-CBA-01", "CM-ROS-02");

        verificar(ubicacion);
    }

    @Test
    void unErrorDelServicioSeTraduceAUbicacionNoDisponible() {
        ServicioGPSTrackerXTR4500L servicioFalso = new ServicioGPSTrackerXTR4500L(null, "sin-red") {
            @Override
            public String getBolsinLocation(String apiKey, int numeroBolsin, String codigoCMOrigen) {
                throw new ErrorServicioGPSTrackerException(404, "codigoComisionMedicaOrigen no coincide con el bolsin solicitado.");
            }
        };
        IAdaptadorGPSTracker adaptador = new AdaptadorGPSTrackerXTR4500L(servicioFalso, "key");

        UbicacionNoDisponibleException error = assertThrows(UbicacionNoDisponibleException.class,
                () -> adaptador.obtenerUbicacion(105, "CM-CBA-01", "CM-TUC-01"));
        assertTrue(error.getMessage().contains("no coincide"));
    }

    @Test
    void unaRespuestaMalFormadaSeTraduceAUbicacionNoDisponible() {
        ServicioGPSTrackerNavTrackQX7A servicioFalso = new ServicioGPSTrackerNavTrackQX7A(null, "sin-red") {
            @Override
            public String retrieveTrackingData(String apiKey, int numeroBolsin, String codigoCMDestino) {
                return "101,-31.4201";
            }
        };
        IAdaptadorGPSTracker adaptador = new AdaptadorGPSTrackerNavTrackQX7A(servicioFalso, "key");

        assertThrows(UbicacionNoDisponibleException.class,
                () -> adaptador.obtenerUbicacion(101, "CM-CBA-01", "CM-ROS-02"));
    }

    private void verificar(UbicacionBolsin ubicacion) {
        assertEquals(101, ubicacion.getNumeroBolsin());
        assertEquals(-31.4201, ubicacion.getLatitud(), 0.000001);
        assertEquals(-64.1888, ubicacion.getLongitud(), 0.000001);
        assertEquals(FECHA, ubicacion.getFechaHoraUltimaActualizacion());
    }
}
