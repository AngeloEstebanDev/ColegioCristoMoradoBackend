package com.CristoMorado_api.CristoMorado_api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

/**
 * Excepción GlobalExceptionHandler: se lanza cuando ocurre este tipo de error. GlobalExceptionHandler
 * la atrapa y responde al frontend con el código HTTP y el mensaje adecuados.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * Atrapa los errores de tipo indicado y responde al frontend con un JSON de error (handle
     * duplicado).
     */
    @ExceptionHandler(RecursoDuplicadoException.class)
    public ResponseEntity<Map<String, String>> handleDuplicado(RecursoDuplicadoException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("mensaje", ex.getMessage()));
    }

    /**
     * Atrapa los errores de tipo indicado y responde al frontend con un JSON de error (handle no
     * encontrado).
     */
    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<Map<String, String>> handleNoEncontrado(RecursoNoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("mensaje", ex.getMessage()));
    }

    /**
     * Atrapa los errores de tipo indicado y responde al frontend con un JSON de error (handle acceso
     * denegado).
     */
    @ExceptionHandler(AccesoDenegadoException.class)
    public ResponseEntity<Map<String, String>> handleAccesoDenegado(AccesoDenegadoException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("mensaje", ex.getMessage()));
    }

    /**
     * Atrapa los errores de tipo indicado y responde al frontend con un JSON de error (handle datos
     * invalidos).
     */
    @ExceptionHandler(DatosInvalidosException.class)
    public ResponseEntity<Map<String, String>> handleDatosInvalidos(DatosInvalidosException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("mensaje", ex.getMessage()));
    }

    /**
     * Atrapa los errores de tipo indicado y responde al frontend con un JSON de error (handle
     * credenciales invalidas).
     */
    @ExceptionHandler(CredencialesInvalidasException.class)
    public ResponseEntity<Map<String, String>> handleCredencialesInvalidas(CredencialesInvalidasException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("mensaje", ex.getMessage()));
    }

    /**
     * Cualquier otro error: se registra en la consola y se responde con un mensaje
     * claro (antes el frontend solo mostraba "No se pudo conectar con el servidor").
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneral(Exception ex) {
        if (ex instanceof org.springframework.web.ErrorResponse er) {
            // Errores propios de Spring (404, 405, JSON mal formado...): se respeta su código.
            return ResponseEntity.status(er.getStatusCode()).body(Map.of("mensaje",
                    ex.getMessage() == null ? "Solicitud no válida." : ex.getMessage()));
        }
        if (ex instanceof org.springframework.http.converter.HttpMessageNotReadableException
                || ex instanceof org.springframework.beans.TypeMismatchException) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("mensaje", "Los datos enviados no tienen el formato correcto."));
        }
        org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler.class).error("Error no controlado", ex);
        String detalle = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("mensaje", "Error interno del servidor: " + detalle));
    }
}
