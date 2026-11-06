package edu.umg.backend.model.repository;

import edu.umg.backend.model.entity.HistorialSesion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistorialRepository extends JpaRepository<HistorialSesion, Long> {

    Optional<HistorialSesion> findByIdAndSesionId(Long id, String sesionId);

    List<HistorialSesion> findBySesionIdOrderByFechaInicioDesc(String sesionId);

    List<HistorialSesion> findBySesionIdAndFechaFinIsNull(String sesionId);

    void deleteByLeccionId(Long leccionId);
}
