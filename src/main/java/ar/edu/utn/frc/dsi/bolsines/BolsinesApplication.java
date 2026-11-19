package ar.edu.utn.frc.dsi.bolsines;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Sistema de Gestión de Bolsines - PPAI 2026 DSI - CU 36 "Consultar seguimiento de bolsines".
 * <p>
 * Antes de ejecutar esta clase hay que levantar la API mock de GPS Tracker de la cátedra
 * (puerto 8080). Esta aplicación queda en http://localhost:8081
 */
@SpringBootApplication
public class BolsinesApplication {

    public static void main(String[] args) {
        SpringApplication.run(BolsinesApplication.class, args);
    }
}
