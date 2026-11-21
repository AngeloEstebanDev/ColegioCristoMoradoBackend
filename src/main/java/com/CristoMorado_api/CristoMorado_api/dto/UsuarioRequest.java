package com.CristoMorado_api.CristoMorado_api.dto;

public record UsuarioRequest(
        String nombre,
        String usuario,
        String dni,
        String password,
        String rol,
        Boolean estado
) {}
