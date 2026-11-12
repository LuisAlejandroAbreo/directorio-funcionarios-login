package com.bootcamp.directorio.service;

import com.bootcamp.directorio.dto.RegistroRequest;
import com.bootcamp.directorio.model.Usuario;
import com.bootcamp.directorio.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

@Service
public class UsuarioService {

    private static final Pattern USERNAME = Pattern.compile("^[a-z0-9._-]{4,20}$");
    private static final Pattern EMAIL = Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final UsuarioRepository repository;
    private final PasswordEncoder encoder;

    public UsuarioService(UsuarioRepository repository, PasswordEncoder encoder) {
        this.repository = repository;
        this.encoder = encoder;
    }

    @Transactional
    public Usuario registrar(RegistroRequest req) {
        String username = Usuario.normalizarUsername(req.username());
        String email = req.email() == null ? "" : req.email().trim().toLowerCase(Locale.ROOT);

        Map<String, List<String>> errores = new LinkedHashMap<>();

        if (username.isEmpty()) {
            errores.put("username", List.of("El usuario es obligatorio."));
        } else if (!USERNAME.matcher(username).matches()) {
            errores.put("username", List.of(
                    "El usuario debe tener entre 4 y 20 caracteres y solo puede incluir letras, números, punto, guion y guion bajo."));
        }

        if (email.isEmpty()) {
            errores.put("email", List.of("El correo es obligatorio."));
        } else if (email.length() > 100 || !EMAIL.matcher(email).matches()) {
            errores.put("email", List.of("Escribe un correo válido (ejemplo: nombre@dominio.com)."));
        }

        List<String> erroresPassword = PasswordValidator.validar(req.password(), username);
        if (!erroresPassword.isEmpty()) {
            errores.put("password", erroresPassword);
        }

        if (!Objects.equals(req.password(), req.confirmarPassword())) {
            errores.put("confirmarPassword", List.of("Las contraseñas no coinciden."));
        }

        if (!errores.isEmpty()) {
            throw new RegistroException(HttpStatus.BAD_REQUEST, errores);
        }

        // Duplicados (409)
        Map<String, List<String>> duplicados = new LinkedHashMap<>();
        if (repository.existsByUsername(username)) {
            duplicados.put("username", new ArrayList<>(List.of("Ese nombre de usuario ya está en uso.")));
        }
        if (repository.existsByEmail(email)) {
            duplicados.put("email", new ArrayList<>(List.of("Ese correo ya está registrado.")));
        }
        if (!duplicados.isEmpty()) {
            throw new RegistroException(HttpStatus.CONFLICT, duplicados);
        }

        Usuario usuario = new Usuario();
        usuario.setUsername(username);
        usuario.setEmail(email);
        usuario.setPassword(encoder.encode(req.password()));
        return repository.save(usuario);
    }
}
