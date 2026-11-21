package com.CristoMorado_api.CristoMorado_api.dto;

public record CodigoEnviadoResponse(
        String correoEnmascarado,
        int minutosValidez,
        int segundosParaReenviar
) {}
