package com.CristoMorado_api.CristoMorado_api.dto;

/**
 * DTOs de dashboard stats: "paquetes" de datos que viajan en JSON entre el frontend y el backend.
 */
public record DashboardStats(
        long usuariosActivos,
        long totalEstudiantes,
        long totalPadres,
        long totalDocentes
) {}
