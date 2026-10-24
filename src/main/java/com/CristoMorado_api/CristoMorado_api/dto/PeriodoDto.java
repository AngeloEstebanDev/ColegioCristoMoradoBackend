package com.CristoMorado_api.CristoMorado_api.dto;

import com.CristoMorado_api.CristoMorado_api.entity.Periodo;

import java.time.LocalDate;

/** Académico > Periodos. */
public final class PeriodoDto {

    private PeriodoDto() {}

    public record Request(String nombre, Integer anio, Integer bimestre, LocalDate fechaInicio, LocalDate fechaFin) {}

    public record Response(Long id, String nombre, Integer anio, Integer bimestre,
                           LocalDate fechaInicio, LocalDate fechaFin, Boolean activo) {
        public static Response fromEntity(Periodo p) {
            return new Response(p.getId(), p.getNombre(), p.getAnio(), p.getBimestre(),
                    p.getFechaInicio(), p.getFechaFin(), Boolean.TRUE.equals(p.getActivo()));
        }
    }
}
