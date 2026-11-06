package edu.umg.backend.controller;

import edu.umg.backend.service.PracticaService;
import java.util.Map;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class PracticaController {

    private final PracticaService practica;

    public PracticaController(PracticaService practica) {
        this.practica = practica;
    }

    @PostMapping("/compilar")
    public Map<String, Object> compilar(@RequestBody Map<String, Object> cuerpo) {
        return practica.compilar(cuerpo);
    }

    @PostMapping("/ejecutar")
    public Map<String, Object> ejecutar(@RequestBody Map<String, Object> cuerpo) {
        return practica.ejecutar(cuerpo);
    }
}
