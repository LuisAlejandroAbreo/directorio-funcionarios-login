package com.bootcamp.directorio.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordValidatorTest {

    @Test
    void aceptaContrasenaFuerte() {
        assertTrue(PasswordValidator.validar("Segura#2026", "ana").isEmpty());
    }

    @Test
    void rechazaContrasenaVaciaConUnSoloMensaje() {
        assertEquals(1, PasswordValidator.validar("", "ana").size());
    }

    @Test
    void reportaTodosLosIncumplimientos() {
        // corta, sin mayúscula, sin número y sin especial
        assertEquals(4, PasswordValidator.validar("abc", "ana").size());
    }

    @Test
    void rechazaEspaciosYUsuarioDentroDeLaClave() {
        assertTrue(PasswordValidator.validar("Con espacio1!", "ana").contains("No puede contener espacios."));
        assertTrue(PasswordValidator.validar("Ana#2026xx", "ana").contains("No puede contener tu nombre de usuario."));
    }
}
