package edu.umg.backend.service;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PracticaService {

    private final CompiladorService compilador;
    private final HistorialService historial;

    public PracticaService(CompiladorService compilador, HistorialService historial) {
        this.compilador = compilador;
        this.historial = historial;
    }

    public Map<String, Object> compilar(Map<String, Object> cuerpo) {
        Map<String, Object> resultado = compilador.compilar(leerCodigo(cuerpo));
        boolean compilo = Boolean.TRUE.equals(resultado.get("exitoso"));
        boolean cumplido = registrar(cuerpo, true, compilo, false);
        return Map.of(
                "exitoso", compilo,
                "salida", resultado.get("salida"),
                "objetivoCumplido", cumplido);
    }

    public Map<String, Object> ejecutar(Map<String, Object> cuerpo) {
        Map<String, Object> resultado = compilador.ejecutar(leerCodigo(cuerpo));
        boolean compilo = Boolean.TRUE.equals(resultado.get("compilacionExitosa"));
        boolean exitosa = Boolean.TRUE.equals(resultado.get("ejecucionExitosa"));
        boolean cumplido = registrar(cuerpo, false, compilo, exitosa);
        return Map.of(
                "compilacionExitosa", compilo,
                "salidaCompilacion", resultado.get("salidaCompilacion"),
                "ejecucionExitosa", exitosa,
                "timeout", resultado.get("timeout"),
                "salida", resultado.get("salida"),
                "errores", resultado.get("errores"),
                "objetivoCumplido", cumplido);
    }

    private String leerCodigo(Map<String, Object> cuerpo) {
        Object codigo = cuerpo.get("codigo");
        if (codigo == null || codigo.toString().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Escribe código antes de continuar.");
        }
        return codigo.toString();
    }

    private boolean registrar(Map<String, Object> cuerpo, boolean esCompilar, boolean compilo, boolean ejecucionExitosa) {
        Object historialId = cuerpo.get("historialId");
        Object sesionId = cuerpo.get("sesionId");
        if (historialId == null || sesionId == null) {
            return false;
        }
        try {
            return historial.registrarIntento(
                    Long.parseLong(historialId.toString()), sesionId.toString(), esCompilar, compilo, ejecucionExitosa);
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
