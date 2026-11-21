package com.CristoMorado_api.CristoMorado_api.dto;

import com.CristoMorado_api.CristoMorado_api.entity.Periodo;

import java.time.LocalDate;

/** Académico > Periodos. */
public final class PeriodoDto {

    /**
     * Constructor de PeriodoDto.
     */
    private PeriodoDto() {}

    /**
     * Estructura de datos Request (record): agrupa request en un solo objeto inmutable que viaja como
     * JSON.
     */
    public record Request(String nombre, Integer anio, Integer bimestre, LocalDate fechaInicio, LocalDate fechaFin) {}

    /**
     * Estructura de datos Response (record): agrupa response en un solo objeto inmutable que viaja
     * como JSON.
     */
    public record Response(Long id, String nombre, Integer anio, Integer bimestre,
                           LocalDate fechaInicio, LocalDate fechaFin, Boolean activo) {
        /**
         * Convierte la entidad en este DTO, copiando solo los datos que necesita el frontend.
         */
        public static Response fromEntity(Periodo p) {
            return new Response(p.getId(), p.getNombre(), p.getAnio(), p.getBimestre(),
                    p.getFechaInicio(), p.getFechaFin(), p.vigenteEn(java.time.LocalDate.now()));
        }
    }
}
