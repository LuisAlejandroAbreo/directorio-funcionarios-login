package com.bootcamp.directorio.dto;

public record RegistroRequest(String username,
                              String email,
                              String password,
                              String confirmarPassword) {
}
