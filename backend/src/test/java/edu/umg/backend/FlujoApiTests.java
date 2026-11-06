package edu.umg.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("h2")
class FlujoApiTests {

    @Value("${local.server.port}")
    int puerto;

    private final HttpClient http = HttpClient.newHttpClient();

    private HttpResponse<String> enviar(String metodo, String ruta, String cuerpo, String basic) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + ruta))
                .header("Content-Type", "application/json")
                .method(metodo, cuerpo == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(cuerpo));
        if (basic != null) {
            b.header("Authorization", "Basic " + Base64.getEncoder().encodeToString(basic.getBytes(StandardCharsets.UTF_8)));
        }
        return http.send(b.build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void estudianteVeLeccionesSinLogin() throws Exception {
        HttpResponse<String> r = enviar("GET", "/api/lecciones", null, null);
        assertEquals(200, r.statusCode());
        assertTrue(r.body().contains("01-hola-mundo"));
    }

    @Test
    void rutasAdminPidenLogin() throws Exception {
        assertEquals(401, enviar("GET", "/api/admin/usuarios", null, null).statusCode());
        assertEquals(401, enviar("GET", "/api/admin/usuarios", null, "admin:mal").statusCode());
        assertEquals(200, enviar("GET", "/api/admin/usuarios", null, "admin:admin123").statusCode());
    }

    @Test
    void historialYObjetivo() throws Exception {
        String sesion = "11111111-1111-1111-1111-111111111111";
        HttpResponse<String> abrir = enviar("POST", "/api/historial", "{\"sesionId\":\"" + sesion + "\",\"leccionId\":1}", null);
        assertEquals(200, abrir.statusCode());
        String id = abrir.body().replaceAll(".*\"id\":(\\d+).*", "$1");

        HttpResponse<String> compilar = enviar("POST", "/api/compilar",
                "{\"codigo\":\"void main(){}\",\"sesionId\":\"" + sesion + "\",\"historialId\":" + id + "}", null);
        assertEquals(200, compilar.statusCode());
        assertTrue(compilar.body().contains("\"objetivoCumplido\":true"));

        HttpResponse<String> lista = enviar("GET", "/api/historial?sesionId=" + sesion, null, null);
        assertTrue(lista.body().contains("\"completada\":true"));
    }

    @Test
    void crearYEliminarAdministrador() throws Exception {
        String auth = "admin:admin123";
        HttpResponse<String> crear = enviar("POST", "/api/admin/usuarios", "{\"usuario\":\"maria\",\"clave\":\"clave12345\"}", auth);
        assertEquals(200, crear.statusCode());
        assertTrue(!crear.body().contains("clave"));
        String id = crear.body().replaceAll(".*\"id\":(\\d+).*", "$1");
        assertEquals(204, enviar("DELETE", "/api/admin/usuarios/" + id, null, auth).statusCode());
    }

    @Test
    void visitaYEstadisticas() throws Exception {
        assertEquals(204, enviar("POST", "/api/visitas", "{\"visitanteId\":\"22222222-2222-2222-2222-222222222222\"}", null).statusCode());
        HttpResponse<String> r = enviar("GET", "/api/admin/estadisticas", null, "admin:admin123");
        assertEquals(200, r.statusCode());
        assertTrue(r.body().contains("visitasTotales"));
    }
}
