package com.CristoMorado_api.CristoMorado_api.exception;

/** El usuario no tiene permiso para esta acción (HTTP 403). */
public class AccesoDenegadoException extends RuntimeException {
    public AccesoDenegadoException(String mensaje) {
        super(mensaje);
    }
}
