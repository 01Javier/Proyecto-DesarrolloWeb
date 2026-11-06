package edu.umg.backend.model.repository;

import edu.umg.backend.model.entity.Visita;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VisitaRepository extends JpaRepository<Visita, Long> {

    boolean existsByVisitanteIdAndFechaAfter(String visitanteId, Instant desde);

    List<Visita> findByFechaGreaterThanEqualAndFechaLessThan(Instant desde, Instant hasta);
}
