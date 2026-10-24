package com.CristoMorado_api.CristoMorado_api.exception;

/** Datos enviados incompletos o con formato incorrecto (HTTP 400). */
public class DatosInvalidosException extends RuntimeException {
    public DatosInvalidosException(String mensaje) {
        super(mensaje);
    }
}
