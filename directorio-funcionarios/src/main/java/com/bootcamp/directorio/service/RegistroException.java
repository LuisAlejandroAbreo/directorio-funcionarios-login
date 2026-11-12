package com.bootcamp.directorio.service;

import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

public class RegistroException extends RuntimeException {

    private final HttpStatus status;
    private final Map<String, List<String>> errores;

    public RegistroException(HttpStatus status, Map<String, List<String>> errores) {
        super("Datos de registro inválidos");
        this.status = status;
        this.errores = errores;
    }

    public HttpStatus getStatus() { return status; }
    public Map<String, List<String>> getErrores() { return errores; }
}
