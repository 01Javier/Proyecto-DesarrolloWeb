package edu.umg.backend.controller;

import edu.umg.backend.service.VisitaService;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class VisitaController {

    private final VisitaService visitas;

    public VisitaController(VisitaService visitas) {
        this.visitas = visitas;
    }

    @PostMapping("/visitas")
    public ResponseEntity<Void> registrar(@RequestBody Map<String, Object> cuerpo) {
        visitas.registrar(cuerpo);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/admin/estadisticas")
    public Map<String, Object> estadisticas(
            @RequestParam(required = false) LocalDate desde,
            @RequestParam(required = false) LocalDate hasta) {
        return visitas.estadisticas(desde, hasta);
    }
}
