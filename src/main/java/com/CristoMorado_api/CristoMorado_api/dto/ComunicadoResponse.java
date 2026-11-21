package com.CristoMorado_api.CristoMorado_api.dto;

import com.CristoMorado_api.CristoMorado_api.entity.Comunicado;
import com.CristoMorado_api.CristoMorado_api.util.RolUtil;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO de salida ComunicadoResponse: datos que el backend devuelve al frontend (en JSON) sobre los
 * comunicados institucionales. Se arma a partir de la entidad para enviar solo lo necesario (por
 * ejemplo, nunca la contraseña).
 */
public record ComunicadoResponse(
        Long id,
        String titulo,
        String contenido,
        String imagen,
        Boolean anuncio,
        List<String> destinatarios,
        List<Long> seccionIds,        // vacío = todo el colegio
        List<String> secciones,       // "3° Primaria A", ...
        Long autorId,
        String autorNombre,
        String autorRol,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaActualizacion,
        Boolean leido,          // para el usuario que consulta
        Long totalLecturas,     // cuántos usuarios lo leyeron
        Boolean editable        // el usuario que consulta puede editarlo/eliminarlo
) {
    /**
     * Convierte la entidad en este DTO, copiando solo los datos que necesita el frontend.
     */
    public static ComunicadoResponse of(Comunicado c, boolean leido, long totalLecturas,
                                        List<String> secciones, boolean editable) {
        return new ComunicadoResponse(
                c.getId(), c.getTitulo(), c.getContenido(), c.getImagen(),
                Boolean.TRUE.equals(c.getAnuncio()),
                List.copyOf(c.getDestinatarios()),
                List.copyOf(c.getSecciones()),
                secciones,
                c.getAutor() == null ? null : c.getAutor().getId(),
                c.getAutor() == null ? "Administración" : c.getAutor().getNombre(),
                c.getAutor() == null ? "Administración" : RolUtil.etiqueta(c.getAutor().getRol()),
                c.getFechaCreacion(), c.getFechaActualizacion(),
                leido, totalLecturas, editable
        );
    }
}
