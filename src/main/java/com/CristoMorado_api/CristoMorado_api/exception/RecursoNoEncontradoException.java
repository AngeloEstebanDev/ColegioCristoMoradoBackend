package com.CristoMorado_api.CristoMorado_api.exception;

/**
 * Excepción RecursoNoEncontradoException: se lanza cuando ocurre este tipo de error.
 * GlobalExceptionHandler la atrapa y responde al frontend con el código HTTP y el mensaje adecuados.
 */
public class RecursoNoEncontradoException extends RuntimeException {
    /**
     * Constructor de RecursoNoEncontradoException.
     */
    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
