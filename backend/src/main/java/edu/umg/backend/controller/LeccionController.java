package edu.umg.backend.controller;

import edu.umg.backend.model.entity.Leccion;
import edu.umg.backend.model.entity.LeccionImagen;
import edu.umg.backend.service.LeccionService;
import edu.umg.backend.service.ZipService;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class LeccionController {

    private final LeccionService lecciones;
    private final ZipService zip;

    public LeccionController(LeccionService lecciones, ZipService zip) {
        this.lecciones = lecciones;
        this.zip = zip;
    }

    @GetMapping("/lecciones")
    public List<Map<String, Object>> listar() {
        return lecciones.listar();
    }

    @GetMapping("/lecciones/{id}")
    public Leccion obtener(@PathVariable Long id) {
        return lecciones.obtener(id);
    }

    @GetMapping("/lecciones/{id}/imagenes/{nombre}")
    public ResponseEntity<byte[]> imagen(@PathVariable Long id, @PathVariable String nombre) {
        LeccionImagen imagen = lecciones.obtenerImagen(id, nombre);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(imagen.getTipoMime()))
                .header(HttpHeaders.CACHE_CONTROL, "max-age=3600")
                .body(imagen.getDatos());
    }

    @PostMapping("/admin/lecciones/importar")
    public Map<String, Object> importar(@RequestParam("archivo") MultipartFile archivo) throws IOException {
        return zip.importar(archivo.getInputStream());
    }

    @GetMapping("/admin/lecciones/exportar")
    public ResponseEntity<byte[]> exportar() throws IOException {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"lecciones.zip\"")
                .body(zip.exportar());
    }

    @DeleteMapping("/admin/lecciones/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        lecciones.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
