package edu.umg.backend.model.repository;

import edu.umg.backend.model.entity.Leccion;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeccionRepository extends JpaRepository<Leccion, Long> {

    Optional<Leccion> findByNombre(String nombre);

    List<Leccion> findAllByOrderByNombreAsc();
}
