package ar.edu.utn.frc.dsi.bolsines.boundary;

import ar.edu.utn.frc.dsi.bolsines.control.SesionUsuario;
import ar.edu.utn.frc.dsi.bolsines.dto.UsuarioDTO;
import ar.edu.utn.frc.dsi.bolsines.entity.Empleado;
import ar.edu.utn.frc.dsi.bolsines.repository.EmpleadoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * SIMULACIÓN del CU 5 "Iniciar sesión" (no forma parte del CU 36).
 * Permite elegir con qué empleado entrar para probar el CU 36 desde distintas CM.
 */
@RestController
@RequestMapping("/api/sesion")
public class PantallaIniciarSesion {

    private final EmpleadoRepository empleadoRepository;
    private final SesionUsuario sesion;

    public PantallaIniciarSesion(EmpleadoRepository empleadoRepository, SesionUsuario sesion) {
        this.empleadoRepository = empleadoRepository;
        this.sesion = sesion;
    }

    @GetMapping("/usuarios")
    public List<UsuarioDTO> listarUsuarios() {
        List<UsuarioDTO> usuarios = new ArrayList<>();
        for (Empleado empleado : empleadoRepository.findAll()) {
            usuarios.add(convertir(empleado));
        }
        usuarios.sort(Comparator.comparing(UsuarioDTO::esEncargadoDeBolsines).reversed()
                .thenComparing(UsuarioDTO::cmNombre));
        return usuarios;
    }

    @PostMapping("/{idEmpleado}")
    public ResponseEntity<?> iniciarSesion(@PathVariable("idEmpleado") Long idEmpleado) {
        Optional<Empleado> empleado = empleadoRepository.findById(idEmpleado);
        if (empleado.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("mensaje", "El usuario no existe."));
        }
        sesion.iniciar(empleado.get());
        return ResponseEntity.ok(convertir(empleado.get()));
    }

    @GetMapping
    public ResponseEntity<?> obtenerSesionActual() {
        if (!sesion.estaIniciada()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("mensaje", "No hay sesión iniciada."));
        }
        return ResponseEntity.ok(convertir(sesion.obtenerEmpleadoLogueado()));
    }

    @PostMapping("/cierre")
    public Map<String, String> cerrarSesion() {
        sesion.cerrar();
        return Map.of("mensaje", "Sesión cerrada.");
    }

    private UsuarioDTO convertir(Empleado empleado) {
        return new UsuarioDTO(
                empleado.getId(),
                empleado.getNombreCompleto(),
                empleado.getRol().getNombre(),
                empleado.sosEncargadoDeBolsines(),
                empleado.getCM().getCodigo(),
                empleado.getCM().getNombre());
    }
}
