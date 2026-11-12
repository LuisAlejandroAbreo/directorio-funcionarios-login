package com.bootcamp.directorio.controller;

import com.bootcamp.directorio.dto.RegistroRequest;
import com.bootcamp.directorio.service.RegistroException;
import com.bootcamp.directorio.service.UsuarioService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UsuarioService usuarioService;

    public AuthController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> registrar(@RequestBody RegistroRequest request) {
        usuarioService.registrar(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("mensaje", "Usuario creado correctamente. Ya puedes iniciar sesión."));
    }

    // Lo usa el front para saber si hay sesión y mostrar el nombre
    @GetMapping("/me")
    public Map<String, String> yo(Authentication authentication) {
        return Map.of("username", authentication.getName());
    }

    @ExceptionHandler(RegistroException.class)
    public ResponseEntity<Map<String, Object>> registroInvalido(RegistroException e) {
        return ResponseEntity.status(e.getStatus()).body(Map.of("errores", e.getErrores()));
    }

    // Respaldo por si dos registros iguales llegan al mismo tiempo (restricción UNIQUE de la BD)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> duplicado(DataIntegrityViolationException e) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Map.of("errores", Map.of("username", List.of("Ese usuario o correo ya está registrado."))));
    }
}
