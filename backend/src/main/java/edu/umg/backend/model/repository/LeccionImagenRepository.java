package edu.umg.backend.model.repository;

import edu.umg.backend.model.entity.LeccionImagen;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeccionImagenRepository extends JpaRepository<LeccionImagen, Long> {

    List<LeccionImagen> findByLeccionId(Long leccionId);

    Optional<LeccionImagen> findFirstByLeccionIdAndNombre(Long leccionId, String nombre);

    void deleteByLeccionId(Long leccionId);
}
