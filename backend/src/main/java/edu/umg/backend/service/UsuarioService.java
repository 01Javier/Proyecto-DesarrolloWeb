package edu.umg.backend.service;

import edu.umg.backend.model.entity.Usuario;
import edu.umg.backend.model.repository.UsuarioRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarios;
    private final PasswordEncoder codificador;

    public UsuarioService(UsuarioRepository usuarios, PasswordEncoder codificador) {
        this.usuarios = usuarios;
        this.codificador = codificador;
    }

    public List<Usuario> listar() {
        return usuarios.findAll();
    }

    @Transactional
    public Usuario crear(Map<String, Object> cuerpo) {
        String nombre = leerNombre(cuerpo);
        String clave = leerClave(cuerpo, true);
        if (usuarios.findByNombreUsuario(nombre).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un usuario con ese nombre.");
        }
        Usuario usuario = new Usuario();
        usuario.setNombreUsuario(nombre);
        usuario.setClave(codificador.encode(clave));
        usuario.setCreadoEn(Instant.now());
        return usuarios.save(usuario);
    }

    @Transactional
    public Usuario actualizar(Long id, Map<String, Object> cuerpo) {
        Usuario usuario = buscar(id);
        String nombre = leerNombre(cuerpo);
        String clave = leerClave(cuerpo, false);
        Usuario otro = usuarios.findByNombreUsuario(nombre).orElse(null);
        if (otro != null && !otro.getId().equals(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un usuario con ese nombre.");
        }
        usuario.setNombreUsuario(nombre);
        if (clave != null) {
            usuario.setClave(codificador.encode(clave));
        }
        return usuario;
    }

    @Transactional
    public void eliminar(Long id, String usuarioActual) {
        Usuario usuario = buscar(id);
        if (usuario.getNombreUsuario().equals(usuarioActual)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No puedes eliminar tu propia cuenta.");
        }
        if (usuarios.count() <= 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Debe existir al menos un administrador.");
        }
        usuarios.delete(usuario);
    }

    private Usuario buscar(Long id) {
        return usuarios.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El usuario no existe."));
    }

    private String leerNombre(Map<String, Object> cuerpo) {
        String nombre = String.valueOf(cuerpo.get("usuario")).strip();
        if (!nombre.matches("[A-Za-z0-9._-]{3,50}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El usuario debe tener de 3 a 50 caracteres: letras, números, punto, guion o guion bajo.");
        }
        return nombre;
    }

    private String leerClave(Map<String, Object> cuerpo, boolean obligatoria) {
        Object valor = cuerpo.get("clave");
        if (valor == null || valor.toString().isEmpty()) {
            if (obligatoria) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contraseña es obligatoria.");
            }
            return null;
        }
        String clave = valor.toString();
        if (clave.length() < 8 || clave.length() > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La contraseña debe tener de 8 a 72 caracteres.");
        }
        return clave;
    }
}
