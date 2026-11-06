package edu.umg.backend.config;

import edu.umg.backend.model.entity.Leccion;
import edu.umg.backend.model.entity.Usuario;
import edu.umg.backend.model.repository.LeccionRepository;
import edu.umg.backend.model.repository.UsuarioRepository;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DatosIniciales implements ApplicationRunner {

    private final UsuarioRepository usuarios;
    private final LeccionRepository lecciones;
    private final PasswordEncoder codificador;
    private final String adminUsuario;
    private final String adminClave;

    public DatosIniciales(UsuarioRepository usuarios, LeccionRepository lecciones, PasswordEncoder codificador,
                          @Value("${guali.admin.usuario}") String adminUsuario,
                          @Value("${guali.admin.clave}") String adminClave) {
        this.usuarios = usuarios;
        this.lecciones = lecciones;
        this.codificador = codificador;
        this.adminUsuario = adminUsuario;
        this.adminClave = adminClave;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (usuarios.count() == 0) {
            Usuario admin = new Usuario();
            admin.setNombreUsuario(adminUsuario);
            admin.setClave(codificador.encode(adminClave));
            admin.setCreadoEn(Instant.now());
            usuarios.save(admin);
        }
        if (lecciones.count() == 0) {
            crear("01-hola-mundo", "COMPILAR_EXITOSO",
                    "# Hola mundo\n\nEscribe un programa que se compile sin errores.\n\n```java\nvoid main() {\n    System.out.println(\"Hola mundo\");\n}\n```\n\nPresiona **Compilar**.");
            crear("02-errores-de-compilacion", "COMPILAR_CON_ERROR",
                    "# Errores de compilación\n\nEscribe código que **no compile**, por ejemplo olvidando un punto y coma.\n\nPresiona **Compilar** y lee el mensaje del compilador.");
            crear("03-tipos-de-datos", "COMPILAR_EXITOSO",
                    "# Tipos de datos\n\nDeclara una variable `int`, una `double` y un `String`, y compila el programa.");
            crear("04-primer-programa", "EJECUTAR_EXITOSO",
                    "# Primer programa\n\nEscribe un programa que imprima tu nombre y **ejecútalo**.\n\n```java\nvoid main() {\n    System.out.println(\"Mi nombre\");\n}\n```");
            crear("05-errores-de-ejecucion", "EJECUTAR_CON_ERROR",
                    "# Errores de ejecución\n\nEscribe un programa que compile pero falle al ejecutarse, por ejemplo accediendo a una posición inexistente de un arreglo.\n\nPresiona **Ejecutar**.");
        }
    }

    private void crear(String nombre, String objetivo, String markdown) {
        Leccion leccion = new Leccion();
        leccion.setNombre(nombre);
        leccion.setObjetivo(objetivo);
        leccion.setMarkdown(markdown);
        lecciones.save(leccion);
    }
}
