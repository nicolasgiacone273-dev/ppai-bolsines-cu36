package ar.edu.utn.frc.dsi.bolsines.repository;

import ar.edu.utn.frc.dsi.bolsines.entity.Empleado;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmpleadoRepository extends JpaRepository<Empleado, Long> {
}
