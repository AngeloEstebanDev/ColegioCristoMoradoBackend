package com.CristoMorado_api.CristoMorado_api.dto;

import java.util.List;

/**
 * DTO de entrada ComunicadoRequest: datos que el frontend envía al backend (en JSON) sobre los
 * comunicados institucionales. Un DTO (Data Transfer Object) es solo un "paquete" de datos: no tiene
 * lógica ni se guarda directamente.
 */
public record ComunicadoRequest(
        String titulo,
        String contenido,
        String imagen,               // opcional: data URL base64 o enlace
        Boolean anuncio,             // true = mensaje grande al entrar
        List<String> destinatarios,  // TODOS, ADMINISTRATIVO, DOCENTES, ALUMNOS, PADRES
        List<Long> secciones         // opcional: limitar a estas secciones (docentes: obligatorio)
) {}
