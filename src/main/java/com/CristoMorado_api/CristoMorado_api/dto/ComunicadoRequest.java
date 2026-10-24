package com.CristoMorado_api.CristoMorado_api.dto;

import java.util.List;

public record ComunicadoRequest(
        String titulo,
        String contenido,
        String imagen,               // opcional: data URL base64 o enlace
        Boolean anuncio,             // true = mensaje grande al entrar
        List<String> destinatarios,  // TODOS, ADMINISTRATIVO, DOCENTES, ALUMNOS, PADRES
        List<Long> secciones         // opcional: limitar a estas secciones (docentes: obligatorio)
) {}
