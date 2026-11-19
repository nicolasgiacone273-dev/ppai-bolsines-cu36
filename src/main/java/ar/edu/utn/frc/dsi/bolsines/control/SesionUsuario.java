package ar.edu.utn.frc.dsi.bolsines.control;

import ar.edu.utn.frc.dsi.bolsines.entity.Empleado;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

/**
 * Sesión del usuario logueado (una instancia por sesión del navegador).
 * <p>
 * El inicio de sesión (CU 5) está simulado: se elige el empleado de una lista.
 * El CU 36 sólo necesita saber quién está logueado para obtener su CM.
 */
@Component
@SessionScope
public class SesionUsuario {

    private Empleado empleadoLogueado;

    public void iniciar(Empleado empleado) {
        this.empleadoLogueado = empleado;
    }

    public void cerrar() {
        this.empleadoLogueado = null;
    }

    public boolean estaIniciada() {
        return empleadoLogueado != null;
    }

    public Empleado obtenerEmpleadoLogueado() {
        return empleadoLogueado;
    }

    /**
     * ¿El usuario logueado es Encargado de Bolsines? Así la pantalla no necesita hablar con
     * el Empleado directamente (principio "no hables con extraños").
     */
    public boolean esEncargadoDeBolsines() {
        return empleadoLogueado != null && empleadoLogueado.sosEncargadoDeBolsines();
    }
}
