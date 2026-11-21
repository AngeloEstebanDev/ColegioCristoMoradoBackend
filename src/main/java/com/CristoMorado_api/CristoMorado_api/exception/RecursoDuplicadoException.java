package com.CristoMorado_api.CristoMorado_api.exception;

/**
 * Excepción RecursoDuplicadoException: se lanza cuando ocurre este tipo de error.
 * GlobalExceptionHandler la atrapa y responde al frontend con el código HTTP y el mensaje adecuados.
 */
public class RecursoDuplicadoException extends RuntimeException {
    /**
     * Constructor de RecursoDuplicadoException.
     */
    public RecursoDuplicadoException(String mensaje) {
        super(mensaje);
    }
}
