package com.CristoMorado_api.CristoMorado_api.dto;

/** Archivos adjuntos (materiales, actividades y entregas). */
public final class ArchivoDto {

    private ArchivoDto() {}

    /** Archivo nuevo enviado por el frontend: contenido en base64 (con o sin prefijo "data:...;base64,"). */
    public record Request(String nombre, String tipo, String contenido) {}

    /** Datos del archivo para mostrar (sin los bytes). */
    public record Info(Long id, String nombre, String tipo, Long tamanio) {
        public static Info de(Long id, String nombre, String tipo, Long tamanio) {
            return id == null ? null : new Info(id, nombre, tipo, tamanio);
        }
    }
}
