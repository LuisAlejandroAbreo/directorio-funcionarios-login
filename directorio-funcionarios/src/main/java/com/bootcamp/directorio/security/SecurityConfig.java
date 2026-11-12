package com.bootcamp.directorio.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

import java.io.IOException;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           LoginSuccessHandler loginSuccessHandler,
                                           LoginFailureHandler loginFailureHandler) throws Exception {
        http
            // CSRF activo: el token viaja en la cookie XSRF-TOKEN y se reenvía en la cabecera X-XSRF-TOKEN
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
            .addFilterAfter(new CsrfCookieFilter(), BasicAuthenticationFilter.class)

            .authorizeHttpRequests(auth -> auth
                // Públicas: pantallas de acceso, estilos y registro
                .requestMatchers("/login.html", "/registro.html", "/style.css", "/auth.js", "/error", "/favicon.ico").permitAll()
                .requestMatchers(HttpMethod.POST, "/login", "/api/auth/register").permitAll()
                // Todo lo demás (directorio, API, index.html) exige sesión iniciada
                .anyRequest().authenticated())

            .formLogin(form -> form
                .loginPage("/login.html")
                .loginProcessingUrl("/login")
                .successHandler(loginSuccessHandler)
                .failureHandler(loginFailureHandler)
                .permitAll())

            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessHandler((request, response, authentication) -> response.setStatus(HttpServletResponse.SC_OK))
                .deleteCookies("JSESSIONID"))

            .exceptionHandling(ex -> ex.authenticationEntryPoint(this::noAutenticado));

        return http.build();
    }

    // La API responde 401 en JSON; las páginas redirigen al login
    private void noAutenticado(HttpServletRequest request, HttpServletResponse response,
                               AuthenticationException exception) throws IOException {
        if (request.getRequestURI().startsWith("/api/")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"mensaje\":\"Debes iniciar sesión.\"}");
        } else {
            response.sendRedirect("/login.html");
        }
    }
}
