package com.CristoMorado_api.CristoMorado_api.dto;

/**
 * DTO de entrada CambiarPasswordRequest: datos que el frontend envía al backend (en JSON) sobre
 * cambiar contraseña. Un DTO (Data Transfer Object) es solo un "paquete" de datos: no tiene lógica ni
 * se guarda directamente.
 */
public record CambiarPasswordRequest(
        String passwordActual,
        String passwordNueva,
        String codigo          // código enviado al Gmail verificado
) {}
