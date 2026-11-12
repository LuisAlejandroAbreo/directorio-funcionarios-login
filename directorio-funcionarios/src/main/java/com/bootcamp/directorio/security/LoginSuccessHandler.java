package com.bootcamp.directorio.security;

import com.bootcamp.directorio.service.IntentosLoginService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
public class LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final IntentosLoginService intentos;
    private final ObjectMapper mapper;

    public LoginSuccessHandler(IntentosLoginService intentos, ObjectMapper mapper) {
        this.intentos = intentos;
        this.mapper = mapper;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        intentos.registrarExito(authentication.getName());   // reinicia el contador

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        mapper.writeValue(response.getWriter(), Map.of("ok", true, "usuario", authentication.getName()));
    }
}
