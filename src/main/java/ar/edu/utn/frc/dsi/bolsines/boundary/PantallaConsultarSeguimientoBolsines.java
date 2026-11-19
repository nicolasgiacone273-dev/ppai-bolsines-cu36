package ar.edu.utn.frc.dsi.bolsines.boundary;

import ar.edu.utn.frc.dsi.bolsines.control.GestorConsultarSeguimientoBolsines;
import ar.edu.utn.frc.dsi.bolsines.control.SesionUsuario;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Pantalla (boundary) del CU 36 "Consultar seguimiento de bolsines".
 * <p>
 * En una aplicación web la pantalla tiene dos partes:
 * <ul>
 *   <li>la vista que ve el usuario: {@code static/seguimiento.html} + {@code static/js/seguimiento.js}
 *       (muestra el mapa, los filtros y las confirmaciones);</li>
 *   <li>esta clase, que recibe cada acción del usuario y se la pasa al Gestor.</li>
 * </ul>
 * Los nombres de los métodos siguen los mensajes del diagrama de secuencia.
 * <p>
 * La Pantalla no guarda al gestor ni a la sesión como atributos: su relación con ellos es de
 * <b>dependencia</b>, no de asociación. En cada acción los pide con {@link #obtenerGestor()} y
 * {@link #obtenerSesion()} y los usa como variables locales.
 */
@RestController
@RequestMapping("/api/seguimiento")
public class PantallaConsultarSeguimientoBolsines {

    /**
     * Devuelve el gestor del CU de la sesión actual. Es un método de búsqueda ({@code @Lookup}):
     * Spring lo reemplaza en tiempo de ejecución para entregar el gestor, así que el cuerpo no se usa.
     */
    @Lookup
    protected GestorConsultarSeguimientoBolsines obtenerGestor() {
        return null;
    }

    /** Devuelve la sesión del usuario actual. Método de búsqueda ({@code @Lookup}), igual que obtenerGestor(). */
    @Lookup
    protected SesionUsuario obtenerSesion() {
        return null;
    }

    /** Paso 1: el EB selecciona la opción para consultar la ubicación de uno o varios bolsines. */
    @PostMapping("/opcion")
    public ResponseEntity<?> opcionConsultarSeguimientoBolsines() {
        GestorConsultarSeguimientoBolsines gestor = obtenerGestor();
        return ejecutar(gestor::opcionConsultarSeguimiento);
    }

    /** Paso 6: filtro por número de precinto y/o CM destino. */
    @GetMapping("/filtro")
    public ResponseEntity<?> tomarFiltros(@RequestParam(name = "precinto", required = false) Integer numeroPrecinto,
                                          @RequestParam(name = "cmDestino", required = false) String codigoCMDestino) {
        GestorConsultarSeguimientoBolsines gestor = obtenerGestor();
        return ejecutar(() -> gestor.tomarFiltros(numeroPrecinto, codigoCMDestino));
    }

    /** Paso 7: el EB selecciona un bolsín del mapa (la respuesta es la pregunta del paso 8). */
    @PostMapping("/seleccion/{numeroBolsin}")
    public ResponseEntity<?> tomarSeleccionBolsin(@PathVariable("numeroBolsin") int numeroBolsin) {
        GestorConsultarSeguimientoBolsines gestor = obtenerGestor();
        return ejecutar(() -> gestor.tomarSeleccionBolsin(numeroBolsin));
    }

    /** Paso 9 (confirma = true) o flujo A5 (confirma = false). */
    @PostMapping("/confirmacion-correo")
    public ResponseEntity<?> tomarConfirmacionEnvioCorreo(@RequestParam(name = "confirma") boolean confirma) {
        GestorConsultarSeguimientoBolsines gestor = obtenerGestor();
        return ejecutar(() -> gestor.tomarConfirmacionEnvioCorreo(confirma));
    }

    /** Flujo alternativo A4: el EB cancela el caso de uso. */
    @PostMapping("/cancelacion")
    public ResponseEntity<?> opcionCancelar() {
        GestorConsultarSeguimientoBolsines gestor = obtenerGestor();
        return ejecutar(() -> {
            gestor.cancelarCU();
            return Map.of("mensaje", "Se canceló la consulta de seguimiento de bolsines.");
        });
    }

    /** Verifica la sesión y traduce los errores del gestor a respuestas HTTP que la vista sabe mostrar. */
    private ResponseEntity<?> ejecutar(Supplier<Object> accion) {
        SesionUsuario sesion = obtenerSesion();
        if (!sesion.estaIniciada()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("mensaje", "Tu sesión no está iniciada. Ingresá de nuevo."));
        }
        if (!sesion.esEncargadoDeBolsines()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("mensaje", "Sólo el Encargado de Bolsines puede consultar el seguimiento."));
        }
        try {
            return ResponseEntity.ok(accion.get());
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("mensaje", e.getMessage()));
        }
    }
}
