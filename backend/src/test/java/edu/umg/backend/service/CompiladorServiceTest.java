package edu.umg.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class CompiladorServiceTest {

    private static final String JDK = System.getProperty("java.home");

    private final CompiladorService servicio = new CompiladorService(JDK, 5, 10000);

    private static final String CORRECTO = """
            public class Main {
                public static void main(String[] args) {
                    System.out.println("hola");
                }
            }
            """;

    @Test
    void compilaCodigoCorrecto() {
        Map<String, Object> resultado = servicio.compilar(CORRECTO);
        assertEquals(true, resultado.get("exitoso"));
    }

    @Test
    void reportaErrorDeCompilacion() {
        String codigo = CORRECTO.replace("\"hola\");", "\"hola\")");
        Map<String, Object> resultado = servicio.compilar(codigo);
        assertEquals(false, resultado.get("exitoso"));
        assertTrue(resultado.get("salida").toString().contains("error"));
    }

    @Test
    void ejecutaYDevuelveLaSalida() {
        Map<String, Object> resultado = servicio.ejecutar(CORRECTO);
        assertEquals(true, resultado.get("ejecucionExitosa"));
        assertEquals("hola", resultado.get("salida").toString().trim());
    }

    @Test
    void reportaErrorDeEjecucion() {
        String codigo = """
                public class Main {
                    public static void main(String[] args) {
                        int[] datos = {1, 2, 3};
                        System.out.println(datos[5]);
                    }
                }
                """;
        Map<String, Object> resultado = servicio.ejecutar(codigo);
        assertEquals(true, resultado.get("compilacionExitosa"));
        assertEquals(false, resultado.get("ejecucionExitosa"));
        assertTrue(resultado.get("errores").toString().contains("ArrayIndexOutOfBoundsException"));
    }

    @Test
    void detieneProgramasQueNoTerminan() {
        CompiladorService rapido = new CompiladorService(JDK, 1, 10000);
        String codigo = """
                public class Main {
                    public static void main(String[] args) {
                        while (true) {
                        }
                    }
                }
                """;
        Map<String, Object> resultado = rapido.ejecutar(codigo);
        assertEquals(true, resultado.get("timeout"));
    }
}
