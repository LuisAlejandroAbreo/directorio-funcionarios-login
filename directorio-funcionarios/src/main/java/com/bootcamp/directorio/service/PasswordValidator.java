package com.bootcamp.directorio.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Reglas de contraseña. Devuelve TODOS los incumplimientos (no solo el primero)
 * para que la persona los corrija de una vez.
 */
public final class PasswordValidator {

    public static final int MIN = 8;
    public static final int MAX = 64;

    private static final Set<String> COMUNES = Set.of(
            "12345678", "123456789", "1234567890", "password", "password1",
            "contrasena", "contrasena1", "qwerty123", "abc12345", "admin123");

    private PasswordValidator() {
    }

    public static List<String> validar(String password, String username) {
        List<String> errores = new ArrayList<>();

        if (password == null || password.isEmpty()) {
            errores.add("La contraseña es obligatoria.");
            return errores;
        }
        if (password.length() < MIN) {
            errores.add("Debe tener al menos " + MIN + " caracteres.");
        }
        if (password.length() > MAX) {
            errores.add("No puede superar los " + MAX + " caracteres.");
        }
        if (password.chars().anyMatch(Character::isWhitespace)) {
            errores.add("No puede contener espacios.");
        }
        if (password.chars().noneMatch(Character::isUpperCase)) {
            errores.add("Debe incluir al menos una letra mayúscula.");
        }
        if (password.chars().noneMatch(Character::isLowerCase)) {
            errores.add("Debe incluir al menos una letra minúscula.");
        }
        if (password.chars().noneMatch(Character::isDigit)) {
            errores.add("Debe incluir al menos un número.");
        }
        if (password.chars().noneMatch(c -> !Character.isLetterOrDigit(c) && !Character.isWhitespace(c))) {
            errores.add("Debe incluir al menos un carácter especial (por ejemplo ! @ # $ % & *).");
        }

        String minuscula = password.toLowerCase(Locale.ROOT);
        if (username != null && username.length() >= 3 && minuscula.contains(username.toLowerCase(Locale.ROOT))) {
            errores.add("No puede contener tu nombre de usuario.");
        }
        if (COMUNES.contains(minuscula)) {
            errores.add("Es una contraseña demasiado común; elige otra.");
        }
        return errores;
    }
}
