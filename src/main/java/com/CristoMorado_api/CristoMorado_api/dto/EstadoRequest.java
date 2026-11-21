package com.CristoMorado_api.CristoMorado_api.dto;

/**
 * DTO de entrada EstadoRequest: datos que el frontend envía al backend (en JSON) sobre estado. Un DTO
 * (Data Transfer Object) es solo un "paquete" de datos: no tiene lógica ni se guarda directamente.
 */
public record EstadoRequest(Boolean estado) {}
