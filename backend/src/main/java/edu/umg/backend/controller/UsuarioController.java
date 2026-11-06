package edu.umg.backend.controller;

import edu.umg.backend.model.entity.Usuario;
import edu.umg.backend.service.UsuarioService;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class UsuarioController {

    private final UsuarioService usuarios;

    public UsuarioController(UsuarioService usuarios) {
        this.usuarios = usuarios;
    }

    @GetMapping("/yo")
    public Map<String, Object> yo(Principal principal) {
        return Map.of("usuario", principal.getName());
    }

    @GetMapping("/usuarios")
    public List<Usuario> listar() {
        return usuarios.listar();
    }

    @PostMapping("/usuarios")
    public Usuario crear(@RequestBody Map<String, Object> cuerpo) {
        return usuarios.crear(cuerpo);
    }

    @PutMapping("/usuarios/{id}")
    public Usuario actualizar(@PathVariable Long id, @RequestBody Map<String, Object> cuerpo) {
        return usuarios.actualizar(id, cuerpo);
    }

    @DeleteMapping("/usuarios/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id, Principal principal) {
        usuarios.eliminar(id, principal.getName());
        return ResponseEntity.noContent().build();
    }
}
