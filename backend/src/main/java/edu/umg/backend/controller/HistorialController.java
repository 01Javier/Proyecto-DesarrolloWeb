package edu.umg.backend.controller;

import edu.umg.backend.model.entity.HistorialSesion;
import edu.umg.backend.service.HistorialService;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/historial")
public class HistorialController {

    private final HistorialService historial;

    public HistorialController(HistorialService historial) {
        this.historial = historial;
    }

    @PostMapping
    public HistorialSesion abrir(@RequestBody Map<String, Object> cuerpo) {
        return historial.abrir(cuerpo);
    }

    @PostMapping("/{id}/cerrar")
    public ResponseEntity<Void> cerrar(@PathVariable Long id, @RequestBody Map<String, Object> cuerpo) {
        historial.cerrar(id, cuerpo);
        return ResponseEntity.noContent().build();
    }

    @GetMapping
    public List<HistorialSesion> listar(@RequestParam String sesionId) {
        return historial.listar(sesionId);
    }
}
