package ar.edu.utn.frc.dsi.bolsines.dto;

/** Datos del empleado para la pantalla de inicio de sesión y la cabecera. */
public record UsuarioDTO(Long idEmpleado,
                         String nombreCompleto,
                         String rol,
                         boolean esEncargadoDeBolsines,
                         String cmCodigo,
                         String cmNombre) {
}
