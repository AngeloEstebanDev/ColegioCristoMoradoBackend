package com.CristoMorado_api.CristoMorado_api.dto;

public final class ArchivoDto {

    private ArchivoDto() {}

    public record Request(String nombre, String tipo, String contenido) {}

    public record Info(Long id, String nombre, String tipo, Long tamanio) {
        public static Info de(Long id, String nombre, String tipo, Long tamanio) {
            return id == null ? null : new Info(id, nombre, tipo, tamanio);
        }
    }
}
