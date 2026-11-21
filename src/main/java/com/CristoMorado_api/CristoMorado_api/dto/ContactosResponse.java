package com.CristoMorado_api.CristoMorado_api.dto;

import java.util.List;

public record ContactosResponse(
        List<Contacto> contactos,
        List<Grupo> grupos
) {
    public record Contacto(Long usuarioId, String nombre, String rol, String detalle) {}

    public record Grupo(String clave, String nombre, String descripcion, List<Long> usuariosIds) {}
}
