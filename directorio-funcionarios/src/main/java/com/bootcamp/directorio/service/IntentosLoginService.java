package com.bootcamp.directorio.service;

import com.bootcamp.directorio.model.Usuario;
import com.bootcamp.directorio.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Cuenta los intentos fallidos de cada usuario y lo bloquea por un tiempo
 * cuando llega al máximo. Los valores salen de application.properties.
 */
@Service
public class IntentosLoginService {

    public record ResultadoFallo(boolean usuarioExiste, boolean bloqueado, int intentosRestantes) {
    }

    private final UsuarioRepository repository;
    private final int maxIntentos;
    private final int bloqueoMinutos;

    public IntentosLoginService(UsuarioRepository repository,
                                @Value("${app.seguridad.max-intentos:3}") int maxIntentos,
                                @Value("${app.seguridad.bloqueo-minutos:5}") int bloqueoMinutos) {
        this.repository = repository;
        this.maxIntentos = maxIntentos;
        this.bloqueoMinutos = bloqueoMinutos;
    }

    public int getMaxIntentos() { return maxIntentos; }
    public int getBloqueoMinutos() { return bloqueoMinutos; }

    @Transactional
    public ResultadoFallo registrarFallo(String username) {
        Optional<Usuario> encontrado = repository.findByUsername(Usuario.normalizarUsername(username));
        if (encontrado.isEmpty()) {
            return new ResultadoFallo(false, false, 0);
        }
        Usuario usuario = encontrado.get();
        if (usuario.estaBloqueado()) {
            return new ResultadoFallo(true, true, 0);
        }

        int intentos = usuario.getIntentosFallidos() + 1;
        if (intentos >= maxIntentos) {
            usuario.setIntentosFallidos(0);
            usuario.setBloqueadoHasta(LocalDateTime.now().plusMinutes(bloqueoMinutos));
            repository.save(usuario);
            return new ResultadoFallo(true, true, 0);
        }
        usuario.setIntentosFallidos(intentos);
        repository.save(usuario);
        return new ResultadoFallo(true, false, maxIntentos - intentos);
    }

    @Transactional
    public void registrarExito(String username) {
        repository.findByUsername(Usuario.normalizarUsername(username)).ifPresent(usuario -> {
            if (usuario.getIntentosFallidos() != 0 || usuario.getBloqueadoHasta() != null) {
                usuario.setIntentosFallidos(0);
                usuario.setBloqueadoHasta(null);
                repository.save(usuario);
            }
        });
    }

    @Transactional(readOnly = true)
    public long segundosRestantes(String username) {
        return repository.findByUsername(Usuario.normalizarUsername(username))
                .map(Usuario::getBloqueadoHasta)
                .map(hasta -> {
                    long ms = Duration.between(LocalDateTime.now(), hasta).toMillis();
                    return ms <= 0 ? 0L : (ms + 999) / 1000;
                })
                .orElse(0L);
    }
}
