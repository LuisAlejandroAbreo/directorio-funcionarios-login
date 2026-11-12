package com.bootcamp.directorio.security;

import com.bootcamp.directorio.model.Usuario;
import com.bootcamp.directorio.repository.UsuarioRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UsuarioDetailsService implements UserDetailsService {

    private final UsuarioRepository repository;

    public UsuarioDetailsService(UsuarioRepository repository) {
        this.repository = repository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Usuario usuario = repository.findByUsername(Usuario.normalizarUsername(username))
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        // accountLocked(true) hace que Spring Security rechace el login con LockedException
        return User.withUsername(usuario.getUsername())
                .password(usuario.getPassword())
                .roles(usuario.getRol())
                .accountLocked(usuario.estaBloqueado())
                .build();
    }
}
