package com.bootcamp.directorio.security;

import com.bootcamp.directorio.service.IntentosLoginService;
import com.bootcamp.directorio.service.IntentosLoginService.ResultadoFallo;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class LoginFailureHandler implements AuthenticationFailureHandler {

    private final IntentosLoginService intentos;
    private final ObjectMapper mapper;

    public LoginFailureHandler(IntentosLoginService intentos, ObjectMapper mapper) {
        this.intentos = intentos;
        this.mapper = mapper;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        String username = request.getParameter("username");
        int status = HttpServletResponse.SC_UNAUTHORIZED;
        long segundos = 0;
        String mensaje;

        if (exception instanceof LockedException) {
            // La cuenta ya estaba bloqueada: se rechaza aunque la contraseña sea correcta
            status = 423;
            segundos = intentos.segundosRestantes(username);
            mensaje = "Tu cuenta está bloqueada temporalmente. Intenta de nuevo en " + formatear(segundos) + ".";

        } else if (exception instanceof BadCredentialsException) {
            ResultadoFallo r = intentos.registrarFallo(username);
            if (r.bloqueado()) {
                status = 423;
                segundos = intentos.segundosRestantes(username);
                mensaje = "Superaste los " + intentos.getMaxIntentos() + " intentos permitidos. "
                        + "Tu cuenta quedó bloqueada por " + intentos.getBloqueoMinutos() + " minutos.";
            } else if (r.usuarioExiste()) {
                int n = r.intentosRestantes();
                mensaje = "Usuario o contraseña incorrectos. Te " + (n == 1 ? "queda 1 intento." : "quedan " + n + " intentos.");
            } else {
                mensaje = "Usuario o contraseña incorrectos.";
            }

        } else {
            status = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
            mensaje = "No fue posible iniciar sesión. Inténtalo de nuevo más tarde.";
        }

        Map<String, Object> cuerpo = new LinkedHashMap<>();
        cuerpo.put("ok", false);
        cuerpo.put("mensaje", mensaje);
        cuerpo.put("segundos", segundos);

        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        mapper.writeValue(response.getWriter(), cuerpo);
    }

    private String formatear(long segundos) {
        return String.format("%d:%02d", segundos / 60, segundos % 60);
    }
}
