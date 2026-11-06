package edu.umg.backend.service;

import edu.umg.backend.model.entity.Leccion;
import edu.umg.backend.model.entity.LeccionImagen;
import edu.umg.backend.model.repository.HistorialRepository;
import edu.umg.backend.model.repository.LeccionImagenRepository;
import edu.umg.backend.model.repository.LeccionRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LeccionService {

    private final LeccionRepository lecciones;
    private final LeccionImagenRepository imagenes;
    private final HistorialRepository historial;

    public LeccionService(LeccionRepository lecciones, LeccionImagenRepository imagenes, HistorialRepository historial) {
        this.lecciones = lecciones;
        this.imagenes = imagenes;
        this.historial = historial;
    }

    public List<Map<String, Object>> listar() {
        List<Map<String, Object>> lista = new ArrayList<>();
        for (Leccion leccion : lecciones.findAllByOrderByNombreAsc()) {
            lista.add(Map.of("id", leccion.getId(), "nombre", leccion.getNombre(), "objetivo", leccion.getObjetivo()));
        }
        return lista;
    }

    public Leccion obtener(Long id) {
        return lecciones.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La lección no existe."));
    }

    public LeccionImagen obtenerImagen(Long leccionId, String nombre) {
        return imagenes.findFirstByLeccionIdAndNombre(leccionId, nombre)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La imagen no existe."));
    }

    @Transactional
    public void eliminar(Long id) {
        Leccion leccion = obtener(id);
        historial.deleteByLeccionId(id);
        imagenes.deleteByLeccionId(id);
        lecciones.delete(leccion);
    }
}
