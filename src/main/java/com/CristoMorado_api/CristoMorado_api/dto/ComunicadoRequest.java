package com.CristoMorado_api.CristoMorado_api.dto;

import java.util.List;

public record ComunicadoRequest(
        String titulo,
        String contenido,
        String imagen,
        Boolean anuncio,
        List<String> destinatarios,
        List<Long> secciones
) {}
