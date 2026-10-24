package com.CristoMorado_api.CristoMorado_api.dto;

public record CambiarPasswordRequest(
        String passwordActual,
        String passwordNueva,
        String codigo          // código enviado al Gmail verificado
) {}
