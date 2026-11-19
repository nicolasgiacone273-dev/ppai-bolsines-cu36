package ar.edu.utn.frc.dsi.bolsines.config;

import ar.edu.utn.frc.dsi.bolsines.entity.Bolsin;
import ar.edu.utn.frc.dsi.bolsines.entity.ComisionMedica;
import ar.edu.utn.frc.dsi.bolsines.entity.Empleado;
import ar.edu.utn.frc.dsi.bolsines.entity.Estado;
import ar.edu.utn.frc.dsi.bolsines.entity.GPSTracker;
import ar.edu.utn.frc.dsi.bolsines.entity.Rol;
import ar.edu.utn.frc.dsi.bolsines.repository.BolsinRepository;
import ar.edu.utn.frc.dsi.bolsines.repository.ComisionMedicaRepository;
import ar.edu.utn.frc.dsi.bolsines.repository.EmpleadoRepository;
import ar.edu.utn.frc.dsi.bolsines.repository.EstadoRepository;
import ar.edu.utn.frc.dsi.bolsines.repository.RolRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Carga los datos de prueba en la base H2 la primera vez que arranca la aplicación
 * (si la base ya tiene bolsines, no hace nada). Para volver a los datos originales,
 * borrar la carpeta "data" y reiniciar.
 * <p>
 * Los números de bolsín y los códigos de CM coinciden con los que tiene precargados la
 * API mock de la cátedra (bolsines 101 a 110), para que los GPS Tracker los encuentren.
 * <p>
 * Escenarios preparados para el usuario "Lucía Ferreyra" (Encargada de Bolsines, CM Córdoba):
 * <ul>
 *   <li>101 (XTR-4500L), 110 (NavTrack QX-7A), 103, 104 y 107 (GeoPulse MTR-900): se ubican en el mapa.</li>
 *   <li>105 (XTR-4500L): la API lo tiene registrado con otra CM origen, así que responde 404 → flujo A3.</li>
 *   <li>102 (Cerrado) y 106 (Recibido en CM destino): no aparecen porque no están en estado Enviado.</li>
 *   <li>109: está Enviado pero su CM origen es Villa María, así que tampoco aparece.</li>
 * </ul>
 * El usuario "Martín Godoy" (Encargado de Bolsines, CM Rosario) no tiene bolsines enviados → flujo A1.
 */
@Component
public class CargaDatosIniciales implements CommandLineRunner {

    private static final Logger LOG = Logger.getLogger(CargaDatosIniciales.class.getName());
    private static final String DOMINIO = "@comisiones-medicas.example";

    private final RolRepository rolRepository;
    private final ComisionMedicaRepository comisionMedicaRepository;
    private final EstadoRepository estadoRepository;
    private final EmpleadoRepository empleadoRepository;
    private final BolsinRepository bolsinRepository;

    private final Map<String, ComisionMedica> cms = new HashMap<>();
    private final Map<String, Estado> estados = new HashMap<>();

    public CargaDatosIniciales(RolRepository rolRepository, ComisionMedicaRepository comisionMedicaRepository,
                               EstadoRepository estadoRepository, EmpleadoRepository empleadoRepository,
                               BolsinRepository bolsinRepository) {
        this.rolRepository = rolRepository;
        this.comisionMedicaRepository = comisionMedicaRepository;
        this.estadoRepository = estadoRepository;
        this.empleadoRepository = empleadoRepository;
        this.bolsinRepository = bolsinRepository;
    }

    @Override
    public void run(String... args) {
        if (bolsinRepository.count() > 0) {
            LOG.info("La base de datos ya tiene datos: no se cargan los datos iniciales.");
            return;
        }
        cargarDatos();
        LOG.info("Datos iniciales cargados en la base H2.");
    }

    private void cargarDatos() {
        // ---------------- Roles ----------------
        Rol encargadoBolsines = rolRepository.save(new Rol(Rol.ENCARGADO_BOLSINES,
                "Gestiona bolsines: estado, retiro, recepción, cierre, reapertura y seguimiento."));
        Rol gerente = rolRepository.save(new Rol(Rol.GERENTE_COMISION_MEDICA,
                "Genera reportes y estadísticas de la comisión médica."));
        rolRepository.save(new Rol("Encargado de Documentación",
                "Gestiona la documentación y los remitos."));

        // ---------------- Comisiones médicas (códigos iguales a los de la API) ----------------
        cm("CM-CBA-01", "Comisión Médica Córdoba", "Av. Colón 1150, Córdoba", "cordoba", "0351 410-2200");
        cm("CM-CBA-02", "Comisión Médica Villa María", "Bv. España 340, Villa María", "villamaria", "0353 452-1100");
        cm("CM-ROS-02", "Comisión Médica Rosario", "Córdoba 1452, Rosario", "rosario", "0341 421-3300");
        cm("CM-MDZ-03", "Comisión Médica Mendoza", "Av. San Martín 1020, Mendoza", "mendoza", "0261 423-4400");
        cm("CM-BA-01", "Comisión Médica Central", "Av. de Mayo 760, C.A.B.A.", "central", "011 4342-5500");
        cm("CM-TUC-01", "Comisión Médica Tucumán", "24 de Septiembre 650, San Miguel de Tucumán", "tucuman", "0381 421-6600");
        cm("CM-SLA-01", "Comisión Médica Salta", "Balcarce 280, Salta", "salta", "0387 431-7700");
        cm("CM-JUJ-01", "Comisión Médica Jujuy", "Belgrano 540, San Salvador de Jujuy", "jujuy", "0388 422-8800");
        cm("CM-NQN-01", "Comisión Médica Neuquén", "Av. Argentina 210, Neuquén", "neuquen", "0299 443-9900");
        cm("CM-MDQ-01", "Comisión Médica Mar del Plata", "San Martín 2850, Mar del Plata", "mardelplata", "0223 491-1000");
        cm("CM-LPL-01", "Comisión Médica La Plata", "Calle 7 nro. 1020, La Plata", "laplata", "0221 421-2000");

        // ---------------- Estados del ámbito Bolsín ----------------
        estado(Estado.CREADO, "El bolsín se registró y se le pueden agregar remitos.");
        estado(Estado.CERRADO, "El bolsín tiene colocado el precinto.");
        estado(Estado.ENVIADO, "El correo retiró el bolsín de la CM origen.");
        estado(Estado.RECIBIDO_EN_CM_DESTINO, "La CM destino registró la recepción.");
        estado(Estado.DE_BAJA, "El bolsín fue eliminado.");

        // ---------------- Empleados ----------------
        Empleado ebCordoba = empleado("Lucía", "Ferreyra", "lucia.ferreyra", "CM-CBA-01", encargadoBolsines);
        Empleado ebVillaMaria = empleado("Joaquín", "Bustos", "joaquin.bustos", "CM-CBA-02", encargadoBolsines);
        empleado("Martín", "Godoy", "martin.godoy", "CM-ROS-02", encargadoBolsines);
        empleado("Ana", "Molina", "gerencia.cordoba", "CM-CBA-01", gerente);
        empleado("Carolina", "Paz", "gerencia.rosario", "CM-ROS-02", gerente);
        empleado("Diego", "Funes", "gerencia.mendoza", "CM-MDZ-03", gerente);
        empleado("Valeria", "Sosa", "gerencia.central", "CM-BA-01", gerente);
        empleado("Sofía", "Aráoz", "gerencia.tucuman", "CM-TUC-01", gerente);
        empleado("Pablo", "Ibarra", "gerencia.neuquen", "CM-NQN-01", gerente);
        empleado("Laura", "Quiroga", "gerencia.salta", "CM-SLA-01", gerente);

        // ---------------- Bolsines ----------------
        // Enviados desde Córdoba: se ven en el mapa
        bolsin(101, 45101, 2350.0, "CM-CBA-01", "CM-ROS-02", "XTR-0001", GPSTracker.MODELO_XTR_4500L, Estado.ENVIADO, ebCordoba);
        bolsin(110, 45110, 1200.0, "CM-CBA-01", "CM-ROS-02", "NAV-0001", GPSTracker.MODELO_NAVTRACK_QX_7A, Estado.ENVIADO, ebCordoba);
        bolsin(103, 45103, 1800.0, "CM-CBA-01", "CM-MDZ-03", "GEO-0001", GPSTracker.MODELO_GEOPULSE_MTR_900, Estado.ENVIADO, ebCordoba);
        bolsin(104, 45104, 3100.0, "CM-CBA-01", "CM-BA-01", "GEO-0002", GPSTracker.MODELO_GEOPULSE_MTR_900, Estado.ENVIADO, ebCordoba);
        bolsin(107, 45107, 950.0, "CM-CBA-01", "CM-NQN-01", "GEO-0003", GPSTracker.MODELO_GEOPULSE_MTR_900, Estado.ENVIADO, ebCordoba);
        // Enviado desde Córdoba, pero la API lo tiene con otra CM origen → A3
        bolsin(105, 45105, 1500.0, "CM-CBA-01", "CM-TUC-01", "XTR-0002", GPSTracker.MODELO_XTR_4500L, Estado.ENVIADO, ebCordoba);
        // De Córdoba, pero no Enviados → no se muestran
        bolsin(102, 45102, 700.0, "CM-CBA-01", "CM-ROS-02", "XTR-0003", GPSTracker.MODELO_XTR_4500L, Estado.CERRADO, ebCordoba);
        bolsin(106, 45106, 1350.0, "CM-CBA-01", "CM-SLA-01", "GEO-0004", GPSTracker.MODELO_GEOPULSE_MTR_900, Estado.RECIBIDO_EN_CM_DESTINO, ebCordoba);
        // Enviado, pero desde otra CM → no se muestra a Córdoba
        bolsin(109, 45109, 2000.0, "CM-CBA-02", "CM-CBA-01", "GEO-0005", GPSTracker.MODELO_GEOPULSE_MTR_900, Estado.ENVIADO, ebVillaMaria);
    }

    // ---------------------------------------------------------------------

    private void cm(String codigo, String nombre, String direccion, String usuarioMail, String telefono) {
        cms.put(codigo, comisionMedicaRepository.save(
                new ComisionMedica(codigo, nombre, direccion, usuarioMail + DOMINIO, telefono)));
    }

    private void estado(String nombre, String descripcion) {
        estados.put(nombre, estadoRepository.save(new Estado(Estado.AMBITO_BOLSIN, nombre, descripcion)));
    }

    private Empleado empleado(String nombre, String apellido, String usuarioMail, String codigoCM, Rol rol) {
        return empleadoRepository.save(new Empleado(nombre, apellido, usuarioMail + DOMINIO, cms.get(codigoCM), rol));
    }

    /**
     * Crea el bolsín con su historial de estados hasta llegar al estado final indicado
     * (Creado → Cerrado → Enviado → Recibido en CM destino).
     */
    private void bolsin(int numero, int precinto, double pesoGramos, String origen, String destino,
                        String serieGPS, String modeloGPS, String estadoFinal, Empleado responsable) {
        LocalDate fecha = LocalDate.of(2026, 3, 6);
        Bolsin bolsin = new Bolsin(numero, precinto, fecha, pesoGramos, cms.get(origen), cms.get(destino),
                new GPSTracker(serieGPS, modeloGPS));

        List<String> recorrido = List.of(Estado.CREADO, Estado.CERRADO, Estado.ENVIADO, Estado.RECIBIDO_EN_CM_DESTINO);
        LocalDateTime fechaHora = fecha.atTime(9, 0).plusMinutes(numero % 100);
        for (String nombreEstado : recorrido) {
            bolsin.crearCEBolsin(estados.get(nombreEstado), fechaHora, responsable);
            if (nombreEstado.equals(estadoFinal)) {
                break;
            }
            fechaHora = fechaHora.plusHours(8);
        }
        bolsinRepository.save(bolsin);
    }
}
