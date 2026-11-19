package ar.edu.utn.frc.dsi.bolsines.repository;

import ar.edu.utn.frc.dsi.bolsines.entity.Bolsin;
import org.springframework.data.jpa.repository.JpaRepository;

/** Acceso a la base de datos para los bolsines (Spring Data JPA genera la implementación). */
public interface BolsinRepository extends JpaRepository<Bolsin, Long> {
}
