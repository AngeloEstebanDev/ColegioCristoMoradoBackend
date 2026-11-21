package com.CristoMorado_api.CristoMorado_api.exception;

/**
 * Excepción CredencialesInvalidasException: se lanza cuando ocurre este tipo de error.
 * GlobalExceptionHandler la atrapa y responde al frontend con el código HTTP y el mensaje adecuados.
 */
public class CredencialesInvalidasException extends RuntimeException {
    /**
     * Constructor de CredencialesInvalidasException.
     */
    public CredencialesInvalidasException(String mensaje) {
        super(mensaje);
    }
}
