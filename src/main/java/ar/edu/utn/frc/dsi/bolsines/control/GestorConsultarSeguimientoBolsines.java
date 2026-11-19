package ar.edu.utn.frc.dsi.bolsines.control;

import ar.edu.utn.frc.dsi.bolsines.adaptador.IAdaptadorGPSTracker;
import ar.edu.utn.frc.dsi.bolsines.adaptador.UbicacionBolsin;
import ar.edu.utn.frc.dsi.bolsines.adaptador.UbicacionNoDisponibleException;
import ar.edu.utn.frc.dsi.bolsines.dto.BolsinSinUbicacionDTO;
import ar.edu.utn.frc.dsi.bolsines.dto.BolsinUbicadoDTO;
import ar.edu.utn.frc.dsi.bolsines.dto.ConsultaEnvioCorreoDTO;
import ar.edu.utn.frc.dsi.bolsines.dto.OpcionCMDTO;
import ar.edu.utn.frc.dsi.bolsines.dto.ResultadoNotificacionDTO;
import ar.edu.utn.frc.dsi.bolsines.dto.ResultadoSeguimientoDTO;
import ar.edu.utn.frc.dsi.bolsines.entity.Bolsin;
import ar.edu.utn.frc.dsi.bolsines.entity.ComisionMedica;
import ar.edu.utn.frc.dsi.bolsines.entity.Empleado;
import ar.edu.utn.frc.dsi.bolsines.repository.BolsinRepository;
import ar.edu.utn.frc.dsi.bolsines.repository.EmpleadoRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Gestor (controlador) del CU 36 "Consultar seguimiento de bolsines".
 * <p>
 * PATRÓN ADAPTER - rol CLIENTE: pide las ubicaciones SIEMPRE a través de la interfaz
 * {@link IAdaptadorGPSTracker}, sin conocer qué API concreta tiene cada modelo de GPS Tracker.
 * Agregar un cuarto modelo sólo requiere un adaptador nuevo: este gestor no cambia
 * (principio abierto/cerrado).
 * <p>
 * Hay una instancia por sesión de usuario, porque guarda el estado de la ejecución del CU
 * (bolsines encontrados, ubicaciones y bolsín seleccionado) entre un paso y el siguiente.
 */
@Component
@SessionScope
public class GestorConsultarSeguimientoBolsines {

    private static final DateTimeFormatter FORMATO_FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    // Colaboradores
    private final SesionUsuario sesion;
    private final BolsinRepository bolsinRepository;
    private final EmpleadoRepository empleadoRepository;
    private final GestorNotificarUbicacionBolsin gestorNotificarUbicacion;
    private final Map<String, IAdaptadorGPSTracker> adaptadoresPorModelo = new HashMap<>();

    // Estado de la ejecución del caso de uso
    private ComisionMedica cmUsuarioLogueado;
    private final List<Bolsin> bolsinesEnviados = new ArrayList<>();
    private final Map<Integer, UbicacionBolsin> ubicacionesPorBolsin = new LinkedHashMap<>();
    private final Map<Integer, String> motivosSinUbicacion = new LinkedHashMap<>();
    private Bolsin bolsinSeleccionado;

    /**
     * Spring inyecta en la lista TODAS las clases que implementan IAdaptadorGPSTracker
     * (hoy: XTR-4500L, NavTrack QX-7A y GeoPulse MTR-900).
     */
    public GestorConsultarSeguimientoBolsines(SesionUsuario sesion,
                                              BolsinRepository bolsinRepository,
                                              EmpleadoRepository empleadoRepository,
                                              GestorNotificarUbicacionBolsin gestorNotificarUbicacion,
                                              List<IAdaptadorGPSTracker> adaptadores) {
        this.sesion = sesion;
        this.bolsinRepository = bolsinRepository;
        this.empleadoRepository = empleadoRepository;
        this.gestorNotificarUbicacion = gestorNotificarUbicacion;
        for (IAdaptadorGPSTracker adaptador : adaptadores) {
            adaptadoresPorModelo.put(adaptador.getModelo(), adaptador);
        }
    }

    // =====================================================================
    // Pasos 1 a 6 del flujo básico
    // =====================================================================

    /** Paso 1: el EB selecciona la opción para consultar la ubicación de uno o varios bolsines. */
    public ResultadoSeguimientoDTO opcionConsultarSeguimiento() {
        limpiarEjecucion();
        buscarCMUsuarioLogueado();          // paso 2
        buscarBolsinesEnviados();           // paso 3
        if (bolsinesEnviados.isEmpty()) {   // flujo alternativo A1
            return new ResultadoSeguimientoDTO(cmUsuarioLogueado.getCodigo(), cmUsuarioLogueado.getNombre(), false,
                    List.of(), List.of(), List.of(),
                    "No hay bolsines en estado Enviado cuya CM origen sea " + cmUsuarioLogueado.getNombre() + ".",
                    "info");
        }
        solicitarUbicacionesAlGPSTracker(); // pasos 4 y 5
        return armarResultado(bolsinesEnviados, mensajeSinUbicacion(), tipoMensajeSinUbicacion()); // paso 6
    }

    /** Paso 2: busca la CM del usuario logueado. */
    private void buscarCMUsuarioLogueado() {
        Empleado empleado = sesion.obtenerEmpleadoLogueado();
        if (empleado == null) {
            throw new IllegalStateException("No hay un usuario logueado.");
        }
        cmUsuarioLogueado = empleado.getCM();
    }

    /** Paso 3: busca los bolsines en estado Enviado cuya CM origen es la CM del usuario logueado. */
    private void buscarBolsinesEnviados() {
        for (Bolsin bolsin : bolsinRepository.findAll()) {
            if (bolsin.esTuCMOrigen(cmUsuarioLogueado) && bolsin.sosEnviado()) {
                bolsinesEnviados.add(bolsin);
            }
        }
        bolsinesEnviados.sort(Comparator.comparing(Bolsin::getNumeroBolsin));
    }

    /**
     * Pasos 4 y 5: solicita al GPS Tracker de cada bolsín sus datos de localización.
     * Aquí se usa el patrón Adapter: el gestor elige el adaptador según el modelo del
     * dispositivo y siempre llama al mismo método de la interfaz Objetivo.
     */
    private void solicitarUbicacionesAlGPSTracker() {
        for (Bolsin bolsin : bolsinesEnviados) {
            String modelo = bolsin.conocerModeloGPSTracker();
            IAdaptadorGPSTracker adaptador = obtenerAdaptador(modelo);
            if (adaptador == null) {
                motivosSinUbicacion.put(bolsin.getNumeroBolsin(),
                        "No hay un adaptador para el modelo de GPS Tracker " + modelo + ".");
                continue;
            }
            try {
                UbicacionBolsin ubicacion = adaptador.obtenerUbicacion(
                        bolsin.getNumeroBolsin(),
                        bolsin.obtenerCodigoCMOrigen(),
                        bolsin.obtenerCodigoCMDestino());
                ubicacionesPorBolsin.put(bolsin.getNumeroBolsin(), ubicacion);
            } catch (UbicacionNoDisponibleException e) {
                // flujo alternativo A3: el GPS Tracker no pudo informar la ubicación
                motivosSinUbicacion.put(bolsin.getNumeroBolsin(), e.getMessage());
            }
        }
    }

    /**
     * Devuelve el adaptador que sabe comunicarse con el modelo de GPS Tracker indicado
     * (o null si no hay ninguno). El Gestor lo recibe siempre como IAdaptadorGPSTracker.
     */
    private IAdaptadorGPSTracker obtenerAdaptador(String modelo) {
        return adaptadoresPorModelo.get(modelo);
    }

    // =====================================================================
    // Paso 6: filtros por número de precinto o CM destino
    // =====================================================================

    public ResultadoSeguimientoDTO tomarFiltros(Integer numeroPrecinto, String codigoCMDestino) {
        verificarConsultaIniciada();
        boolean filtraPorCM = codigoCMDestino != null && !codigoCMDestino.isBlank();
        List<Bolsin> filtrados = new ArrayList<>();
        for (Bolsin bolsin : bolsinesEnviados) {
            boolean cumplePrecinto = numeroPrecinto == null || bolsin.tenesPrecinto(numeroPrecinto);
            boolean cumpleCM = !filtraPorCM || bolsin.tenesCMDestino(codigoCMDestino);
            if (cumplePrecinto && cumpleCM) {
                filtrados.add(bolsin);
            }
        }
        if (numeroPrecinto != null && filtrados.isEmpty()) {
            // flujo alternativo A2: no se encuentra bolsín con el número de precinto ingresado
            return armarResultado(bolsinesEnviados,
                    "No se encontró ningún bolsín enviado con el número de precinto " + numeroPrecinto
                            + (filtraPorCM ? " para la CM destino elegida" : "") + ".",
                    "advertencia");
        }
        return armarResultado(filtrados, null, null);
    }

    // =====================================================================
    // Pasos 7 a 11
    // =====================================================================

    /** Paso 7: el EB selecciona un bolsín del mapa. Paso 8: se le consulta si quiere enviar el correo. */
    public ConsultaEnvioCorreoDTO tomarSeleccionBolsin(int numeroBolsin) {
        verificarConsultaIniciada();
        UbicacionBolsin ubicacion = ubicacionesPorBolsin.get(numeroBolsin);
        Bolsin bolsin = buscarBolsinEnviado(numeroBolsin);
        if (bolsin == null || ubicacion == null) {
            throw new IllegalArgumentException("El bolsín " + numeroBolsin + " no está en el mapa.");
        }
        bolsinSeleccionado = bolsin;
        String nombreCMDestino = bolsin.obtenerCMDestino().getNombre();
        return new ConsultaEnvioCorreoDTO(
                numeroBolsin,
                bolsin.getNroPrecinto(),
                nombreCMDestino,
                ubicacion.getFechaHoraUltimaActualizacion().format(FORMATO_FECHA_HORA),
                ubicacion.getLatitud(),
                ubicacion.getLongitud(),
                "¿Querés enviar un correo al Gerente de " + nombreCMDestino
                        + " para informarle la ubicación del bolsín " + numeroBolsin + "?");
    }

    /** Paso 9 (o A5 si no confirma). Si confirma: pasos 10 y 11. */
    public ResultadoNotificacionDTO tomarConfirmacionEnvioCorreo(boolean confirma) {
        verificarConsultaIniciada();
        if (bolsinSeleccionado == null) {
            throw new IllegalStateException("Primero seleccioná un bolsín del mapa.");
        }
        if (!confirma) {
            // flujo alternativo A5: el EB no selecciona la opción para enviar el correo
            int numero = bolsinSeleccionado.getNumeroBolsin();
            bolsinSeleccionado = null;
            return new ResultadoNotificacionDTO(false, false,
                    "No se envió el correo del bolsín " + numero + ". Podés seleccionar otro bolsín del mapa.",
                    null, null);
        }
        String emailGCM = buscarEmailGCMDestino(); // paso 10
        ComisionMedica cmDestino = bolsinSeleccionado.obtenerCMDestino();
        if (emailGCM == null) {
            return new ResultadoNotificacionDTO(false, false,
                    cmDestino.getNombre() + " no tiene un Gerente con correo registrado. No se pudo notificar.",
                    null, null);
        }
        UbicacionBolsin ubicacion = ubicacionesPorBolsin.get(bolsinSeleccionado.getNumeroBolsin());
        // paso 11: se incluye el CU 30 "Notificar ubicación de bolsín"
        String copia = gestorNotificarUbicacion.notificarUbicacionBolsin(
                emailGCM,
                cmDestino.getNombre(),
                ubicacion.getNumeroBolsin(),
                ubicacion.getFechaHoraUltimaActualizacion(),
                ubicacion.getLatitud(),
                ubicacion.getLongitud());
        String mensaje = "Se envió el correo con la ubicación del bolsín " + ubicacion.getNumeroBolsin()
                + " al Gerente de " + cmDestino.getNombre() + ".";
        finCU();
        return new ResultadoNotificacionDTO(true, true, mensaje, emailGCM, copia);
    }

    /** Paso 10: busca la dirección de correo del Gerente de la CM destino. */
    private String buscarEmailGCMDestino() {
        ComisionMedica cmDestino = bolsinSeleccionado.obtenerCMDestino();
        for (Empleado empleado : empleadoRepository.findAll()) {
            if (empleado.esTuCM(cmDestino) && empleado.sosGCM()) {
                return empleado.getEmail();
            }
        }
        return null;
    }

    /** Flujo alternativo A4: el EB cancela la ejecución del caso de uso. */
    public void cancelarCU() {
        finCU();
    }

    public void finCU() {
        limpiarEjecucion();
    }

    // =====================================================================
    // Auxiliares
    // =====================================================================

    private ResultadoSeguimientoDTO armarResultado(List<Bolsin> bolsines, String mensaje, String tipoMensaje) {
        List<BolsinUbicadoDTO> ubicados = new ArrayList<>();
        List<BolsinSinUbicacionDTO> sinUbicacion = new ArrayList<>();
        for (Bolsin bolsin : bolsines) {
            UbicacionBolsin ubicacion = ubicacionesPorBolsin.get(bolsin.getNumeroBolsin());
            if (ubicacion != null) {
                ubicados.add(new BolsinUbicadoDTO(
                        bolsin.getNumeroBolsin(),
                        bolsin.getNroPrecinto(),
                        bolsin.obtenerCodigoCMDestino(),
                        bolsin.obtenerCMDestino().getNombre(),
                        bolsin.conocerModeloGPSTracker(),
                        ubicacion.getLatitud(),
                        ubicacion.getLongitud(),
                        ubicacion.getFechaHoraUltimaActualizacion().format(FORMATO_FECHA_HORA)));
            } else {
                sinUbicacion.add(new BolsinSinUbicacionDTO(
                        bolsin.getNumeroBolsin(),
                        bolsin.getNroPrecinto(),
                        bolsin.obtenerCMDestino().getNombre(),
                        bolsin.conocerModeloGPSTracker(),
                        motivosSinUbicacion.get(bolsin.getNumeroBolsin())));
            }
        }
        return new ResultadoSeguimientoDTO(cmUsuarioLogueado.getCodigo(), cmUsuarioLogueado.getNombre(), true,
                ubicados, sinUbicacion, armarOpcionesCMDestino(), mensaje, tipoMensaje);
    }

    private List<OpcionCMDTO> armarOpcionesCMDestino() {
        Map<String, OpcionCMDTO> opciones = new LinkedHashMap<>();
        for (Bolsin bolsin : bolsinesEnviados) {
            ComisionMedica destino = bolsin.obtenerCMDestino();
            opciones.putIfAbsent(destino.getCodigo(), new OpcionCMDTO(destino.getCodigo(), destino.getNombre()));
        }
        List<OpcionCMDTO> lista = new ArrayList<>(opciones.values());
        lista.sort(Comparator.comparing(OpcionCMDTO::nombre));
        return lista;
    }

    private String mensajeSinUbicacion() {
        if (motivosSinUbicacion.isEmpty()) {
            return null;
        }
        if (ubicacionesPorBolsin.isEmpty()) {
            return "Ningún GPS Tracker pudo informar la ubicación de los bolsines enviados.";
        }
        int cantidad = motivosSinUbicacion.size();
        return cantidad == 1
                ? "Hay 1 bolsín cuyo GPS Tracker no pudo informar la ubicación."
                : "Hay " + cantidad + " bolsines cuyo GPS Tracker no pudo informar la ubicación.";
    }

    private String tipoMensajeSinUbicacion() {
        return motivosSinUbicacion.isEmpty() ? null : "advertencia";
    }

    private Bolsin buscarBolsinEnviado(int numeroBolsin) {
        for (Bolsin bolsin : bolsinesEnviados) {
            if (bolsin.getNumeroBolsin() == numeroBolsin) {
                return bolsin;
            }
        }
        return null;
    }

    private void verificarConsultaIniciada() {
        if (cmUsuarioLogueado == null) {
            throw new IllegalStateException("La consulta no está iniciada. Volvé a abrir el seguimiento de bolsines.");
        }
    }

    private void limpiarEjecucion() {
        cmUsuarioLogueado = null;
        bolsinesEnviados.clear();
        ubicacionesPorBolsin.clear();
        motivosSinUbicacion.clear();
        bolsinSeleccionado = null;
    }
}
