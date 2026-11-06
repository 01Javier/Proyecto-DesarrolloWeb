package edu.umg.backend.service;

import edu.umg.guali.compiler.CompileResult;
import edu.umg.guali.compiler.ExecuteResult;
import edu.umg.guali.compiler.GualiDev;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CompiladorService {

    private final ReentrantLock candado = new ReentrantLock(true);
    private final String jdkHome;
    private final int timeoutSegundos;
    private final int maxSalida;
    private final String carpetaTemporal;

    public CompiladorService(
            @Value("${guali.jdk-home:}") String jdkHome,
            @Value("${guali.timeout-segundos:5}") int timeoutSegundos,
            @Value("${guali.max-salida:10000}") int maxSalida) {
        this.jdkHome = jdkHome;
        this.timeoutSegundos = timeoutSegundos;
        this.maxSalida = maxSalida;
        this.carpetaTemporal = Path.of(System.getProperty("java.io.tmpdir"), "guali-dev") + File.separator;

        if (jdkHome == null || jdkHome.isBlank()) {
            throw new IllegalStateException("Define GUALI_JDK_HOME con la ruta de un JDK 25.");
        }
        Path bin = Path.of(jdkHome, "bin");
        if (!Files.isRegularFile(bin.resolve("javac")) && !Files.isRegularFile(bin.resolve("javac.exe"))) {
            throw new IllegalStateException("No se encontró javac en " + bin + ". Se necesita un JDK, no un JRE.");
        }
    }

    public Map<String, Object> compilar(String codigo) {
        validar(codigo);
        esperarTurno();
        try {
            CompileResult resultado = GualiDev.compile(codigo, jdkHome);
            return Map.of("exitoso", resultado.isExitoso(), "salida", limpiar(resultado.getSalida()));
        } finally {
            candado.unlock();
        }
    }

    public Map<String, Object> ejecutar(String codigo) {
        validar(codigo);
        esperarTurno();
        try {
            CompileResult compilacion = GualiDev.compile(codigo, jdkHome);
            if (!compilacion.isExitoso()) {
                return Map.of(
                        "compilacionExitosa", false,
                        "salidaCompilacion", limpiar(compilacion.getSalida()),
                        "ejecucionExitosa", false,
                        "timeout", false,
                        "salida", "",
                        "errores", "");
            }
            ExecuteResult ejecucion = GualiDev.execute(compilacion.getNombreClase(), jdkHome, timeoutSegundos);
            return Map.of(
                    "compilacionExitosa", true,
                    "salidaCompilacion", "",
                    "ejecucionExitosa", ejecucion.isExitoso(),
                    "timeout", ejecucion.isTimeoutAlcanzado(),
                    "salida", limpiar(ejecucion.getSalida()),
                    "errores", limpiar(ejecucion.getErrores()));
        } finally {
            candado.unlock();
        }
    }

    private void validar(String codigo) {
        if (codigo == null || codigo.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El código está vacío.");
        }
        if (codigo.length() > 20000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El código supera los 20000 caracteres.");
        }
    }

    private void esperarTurno() {
        try {
            if (!candado.tryLock(30, TimeUnit.SECONDS)) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "El compilador está ocupado.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "El compilador está ocupado.");
        }
    }

    private String limpiar(String texto) {
        if (texto == null) {
            return "";
        }
        String resultado = texto.replace(carpetaTemporal, "");
        if (resultado.length() > maxSalida) {
            resultado = resultado.substring(0, maxSalida) + "\n... (salida recortada)";
        }
        return resultado;
    }
}
