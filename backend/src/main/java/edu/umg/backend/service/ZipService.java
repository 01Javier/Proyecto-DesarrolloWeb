package edu.umg.backend.service;

import edu.umg.backend.model.entity.Leccion;
import edu.umg.backend.model.entity.LeccionImagen;
import edu.umg.backend.model.repository.LeccionImagenRepository;
import edu.umg.backend.model.repository.LeccionRepository;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ZipService {

    private static final List<String> OBJETIVOS =
            List.of("COMPILAR_CON_ERROR", "COMPILAR_EXITOSO", "EJECUTAR_CON_ERROR", "EJECUTAR_EXITOSO");
    private static final Map<String, String> TIPOS_IMAGEN = Map.of(
            "png", "image/png",
            "jpg", "image/jpeg",
            "jpeg", "image/jpeg",
            "gif", "image/gif",
            "webp", "image/webp");

    private final LeccionRepository lecciones;
    private final LeccionImagenRepository imagenes;

    public ZipService(LeccionRepository lecciones, LeccionImagenRepository imagenes) {
        this.lecciones = lecciones;
        this.imagenes = imagenes;
    }

    @Transactional
    public Map<String, Object> importar(InputStream entrada) throws IOException {
        Map<String, Map<String, byte[]>> carpetas = new LinkedHashMap<>();
        try (ZipInputStream zip = new ZipInputStream(entrada, StandardCharsets.UTF_8)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }
                String[] partes = entry.getName().replace('\\', '/').split("/");
                if (partes.length < 2 || partes[0].equals("__MACOSX")) {
                    continue;
                }
                String carpeta = partes[partes.length - 2];
                String archivo = partes[partes.length - 1];
                carpetas.computeIfAbsent(carpeta, k -> new LinkedHashMap<>()).put(archivo, zip.readAllBytes());
            }
        }

        List<String> importadas = new ArrayList<>();
        List<String> errores = new ArrayList<>();
        for (Map.Entry<String, Map<String, byte[]>> carpeta : carpetas.entrySet()) {
            String nombre = carpeta.getKey();
            Map<String, byte[]> archivos = carpeta.getValue();
            byte[] markdown = archivos.get("leccion.md");
            byte[] config = archivos.get("config.txt");
            if (markdown == null && config == null) {
                continue;
            }
            if (markdown == null || config == null) {
                errores.add(nombre + ": deben existir leccion.md y config.txt");
                continue;
            }
            String objetivo = leerObjetivo(new String(config, StandardCharsets.UTF_8));
            if (objetivo == null) {
                errores.add(nombre + ": config.txt debe tener una sola línea, por ejemplo objetivo=EJECUTAR_EXITOSO");
                continue;
            }
            if (nombre.length() > 150) {
                errores.add(nombre + ": el nombre de la carpeta es demasiado largo");
                continue;
            }
            guardar(nombre, new String(markdown, StandardCharsets.UTF_8), objetivo, archivos);
            importadas.add(nombre);
        }

        if (importadas.isEmpty() && errores.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El zip no contiene lecciones con el formato esperado.");
        }
        return Map.of("importadas", importadas, "errores", errores);
    }

    @Transactional(readOnly = true)
    public byte[] exportar() throws IOException {
        List<Leccion> todas = lecciones.findAllByOrderByNombreAsc();
        if (todas.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No hay lecciones para exportar.");
        }
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(salida, StandardCharsets.UTF_8)) {
            for (Leccion leccion : todas) {
                String carpeta = "lecciones/" + leccion.getNombre() + "/";
                agregar(zip, carpeta + "leccion.md", leccion.getMarkdown().getBytes(StandardCharsets.UTF_8));
                agregar(zip, carpeta + "config.txt", ("objetivo=" + leccion.getObjetivo() + "\n").getBytes(StandardCharsets.UTF_8));
                for (LeccionImagen imagen : imagenes.findByLeccionId(leccion.getId())) {
                    agregar(zip, carpeta + imagen.getNombre(), imagen.getDatos());
                }
            }
        }
        return salida.toByteArray();
    }

    private String leerObjetivo(String texto) {
        String limpio = texto.replace("﻿", "").strip();
        if (limpio.lines().count() != 1 || !limpio.startsWith("objetivo=")) {
            return null;
        }
        String valor = limpio.substring("objetivo=".length()).strip();
        return OBJETIVOS.contains(valor) ? valor : null;
    }

    private void guardar(String nombre, String markdown, String objetivo, Map<String, byte[]> archivos) {
        Leccion leccion = lecciones.findByNombre(nombre).orElseGet(Leccion::new);
        leccion.setNombre(nombre);
        leccion.setMarkdown(markdown);
        leccion.setObjetivo(objetivo);
        lecciones.save(leccion);

        imagenes.deleteByLeccionId(leccion.getId());
        for (Map.Entry<String, byte[]> archivo : archivos.entrySet()) {
            String nombreArchivo = archivo.getKey();
            int punto = nombreArchivo.lastIndexOf('.');
            String tipo = punto < 0 ? null : TIPOS_IMAGEN.get(nombreArchivo.substring(punto + 1).toLowerCase(Locale.ROOT));
            if (tipo == null) {
                continue;
            }
            LeccionImagen imagen = new LeccionImagen();
            imagen.setLeccion(leccion);
            imagen.setNombre(nombreArchivo);
            imagen.setTipoMime(tipo);
            imagen.setDatos(archivo.getValue());
            imagenes.save(imagen);
        }
    }

    private void agregar(ZipOutputStream zip, String ruta, byte[] datos) throws IOException {
        zip.putNextEntry(new ZipEntry(ruta));
        zip.write(datos);
        zip.closeEntry();
    }
}
