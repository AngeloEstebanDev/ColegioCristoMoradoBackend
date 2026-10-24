package com.CristoMorado_api.CristoMorado_api.dto;

import java.util.List;

/** Personas y grupos a los que el usuario logeado puede escribir. */
public record ContactosResponse(
        List<Contacto> contactos,
        List<Grupo> grupos
) {
    /** detalle: p.ej. "5° A" para un alumno o la especialidad de un docente. */
    public record Contacto(Long usuarioId, String nombre, String rol, String detalle) {}

    /** Grupo rápido (p.ej. "Salón 5° A") que se expande a sus integrantes. */
    public record Grupo(String clave, String nombre, String descripcion, List<Long> usuariosIds) {}
}
