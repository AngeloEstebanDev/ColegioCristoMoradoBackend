package com.CristoMorado_api.CristoMorado_api.dto;

/**
 * Respuesta al pedir un código de verificación. El código solo llega al Gmail del usuario:
 * nunca se devuelve en la respuesta ni se muestra en pantalla.
 */
public record CodigoEnviadoResponse(
        String correoEnmascarado,   // p.ej. "lu*****23@gmail.com"
        int minutosValidez,
        int segundosParaReenviar
) {}
