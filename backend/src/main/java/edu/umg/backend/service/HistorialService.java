package edu.umg.backend.service;

import edu.umg.backend.model.entity.HistorialSesion;
import edu.umg.backend.model.entity.Leccion;
import edu.umg.backend.model.repository.HistorialRepository;
import edu.umg.backend.model.repository.LeccionRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class HistorialService {

    private final HistorialRepository historial;
    private final LeccionRepository lecciones;

    public HistorialService(HistorialRepository historial, LeccionRepository lecciones) {
        this.historial = historial;
        this.lecciones = lecciones;
    }

    @Transactional
    public HistorialSesion abrir(Map<String, Object> cuerpo) {
        String sesionId = validarSesion(cuerpo.get("sesionId"));
        Long leccionId = convertirId(cuerpo.get("leccionId"));
        Leccion leccion = lecciones.findById(leccionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La lección no existe."));

        Instant ahora = Instant.now();
        for (HistorialSesion abierto : historial.findBySesionIdAndFechaFinIsNull(sesionId)) {
            abierto.setFechaFin(ahora);
        }

        HistorialSesion registro = new HistorialSesion();
        registro.setSesionId(sesionId);
        registro.setLeccion(leccion);
        registro.setLeccionNombre(leccion.getNombre());
        registro.setFechaInicio(ahora);
        registro.setCompletada(false);
        return historial.save(registro);
    }

    @Transactional
    public void cerrar(Long id, Map<String, Object> cuerpo) {
        String sesionId = validarSesion(cuerpo.get("sesionId"));
        HistorialSesion registro = historial.findByIdAndSesionId(id, sesionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El registro no existe."));
        if (registro.getFechaFin() == null) {
            registro.setFechaFin(Instant.now());
        }
    }

    public List<HistorialSesion> listar(String sesionId) {
        return historial.findBySesionIdOrderByFechaInicioDesc(validarSesion(sesionId));
    }

    @Transactional
    public boolean registrarIntento(Long id, String sesionId, boolean esCompilar, boolean compilo, boolean ejecucionExitosa) {
        HistorialSesion registro = historial.findByIdAndSesionId(id, sesionId).orElse(null);
        if (registro == null) {
            return false;
        }
        boolean cumplido = cumpleObjetivo(registro.getLeccion().getObjetivo(), esCompilar, compilo, ejecucionExitosa);
        if (cumplido && !registro.isCompletada() && registro.getFechaFin() == null) {
            registro.setCompletada(true);
            registro.setFechaFin(Instant.now());
        }
        return cumplido;
    }

    private boolean cumpleObjetivo(String objetivo, boolean esCompilar, boolean compilo, boolean ejecucionExitosa) {
        return switch (objetivo) {
            case "COMPILAR_CON_ERROR" -> esCompilar && !compilo;
            case "COMPILAR_EXITOSO" -> esCompilar && compilo;
            case "EJECUTAR_CON_ERROR" -> !esCompilar && compilo && !ejecucionExitosa;
            case "EJECUTAR_EXITOSO" -> !esCompilar && compilo && ejecucionExitosa;
            default -> false;
        };
    }

    private String validarSesion(Object valor) {
        String sesionId = String.valueOf(valor);
        if (!sesionId.matches("[0-9a-fA-F-]{36}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El identificador de sesión no es válido.");
        }
        return sesionId;
    }

    private Long convertirId(Object valor) {
        try {
            return Long.parseLong(String.valueOf(valor));
        } catch (NumberFormatException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El identificador de la lección no es válido.");
        }
    }
}
