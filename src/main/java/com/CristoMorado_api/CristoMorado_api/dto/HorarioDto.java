package com.CristoMorado_api.CristoMorado_api.dto;

import java.util.List;

/** Editor de horario de una sección. Las horas viajan como texto "HH:mm". */
public final class HorarioDto {

    private HorarioDto() {}

    /** Lo que envía el editor al pulsar "Guardar" (reemplaza todo el horario). */
    public record BloqueRequest(Long seccionCursoId, Integer dia, String horaInicio, String horaFin) {}

    public record GuardarRequest(List<BloqueRequest> bloques) {}

    public record Bloque(Long id, Long seccionCursoId, String curso, String color, String docente,
                         String estadoCurso, Integer dia, String horaInicio, String horaFin) {}

    public record Respuesta(Long seccionId, String grado, String letra, String turno,
                            String inicio, String fin, String recesoInicio, String recesoFin,
                            int minutosBloque, List<Bloque> bloques) {}
}
