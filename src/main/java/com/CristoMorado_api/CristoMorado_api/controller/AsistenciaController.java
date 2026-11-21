package com.CristoMorado_api.CristoMorado_api.controller;

import com.CristoMorado_api.CristoMorado_api.dto.AsistenciaDto;
import com.CristoMorado_api.CristoMorado_api.services.AsistenciaService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/** HU-18 Registro de asistencia. Fechas "yyyy-MM-dd". */
@RestController
@RequestMapping("/api/asistencia")
@CrossOrigin(origins = "${app.cors.origenes}")
public class AsistenciaController {

    private static final String USUARIO = "X-Usuario-Id";

    private final AsistenciaService asistenciaService;

    /**
     * Constructor: Spring entrega (inyecta) automáticamente las dependencias que usa esta clase.
     */
    public AsistenciaController(AsistenciaService asistenciaService) {
        this.asistenciaService = asistenciaService;
    }

    /**
     * Endpoint GET /api/asistencia/cursos/{seccionCursoId} — Devuelve día. La lógica está en
     * asistenciaService.dia().
     */
    @GetMapping("/cursos/{seccionCursoId}")
    public AsistenciaDto.Dia dia(@PathVariable Long seccionCursoId,
                                 @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                                 @RequestHeader(value = USUARIO, required = false) Long usuarioId) {
        return asistenciaService.dia(usuarioId, seccionCursoId, fecha);
    }

    /**
     * Endpoint PUT /api/asistencia/cursos/{seccionCursoId} — Guarda la asistencia de los alumnos. La
     * lógica está en asistenciaService.guardar().
     */
    @PutMapping("/cursos/{seccionCursoId}")
    public AsistenciaDto.Dia guardar(@PathVariable Long seccionCursoId, @RequestBody AsistenciaDto.GuardarRequest req,
                                     @RequestHeader(value = USUARIO, required = false) Long usuarioId) {
        return asistenciaService.guardar(usuarioId, seccionCursoId, req);
    }

    /** Clases del curso con su estado de asistencia (marcada, sin marcar, no generada). */
    @GetMapping("/cursos/{seccionCursoId}/sesiones")
    public AsistenciaDto.Sesiones sesiones(@PathVariable Long seccionCursoId,
                                           @RequestHeader(value = USUARIO, required = false) Long usuarioId) {
        return asistenciaService.sesiones(usuarioId, seccionCursoId);
    }

    /** Genera la asistencia de la clase de hoy. Solo funciona durante la hora de clase. */
    @PostMapping("/cursos/{seccionCursoId}/sesiones")
    public AsistenciaDto.Sesiones generar(@PathVariable Long seccionCursoId,
                                          @RequestHeader(value = USUARIO, required = false) Long usuarioId) {
        return asistenciaService.generar(usuarioId, seccionCursoId);
    }

    /**
     * Endpoint GET /api/asistencia/cursos/{seccionCursoId}/resumen — Devuelve resumen. La lógica está
     * en asistenciaService.resumenCurso().
     */
    @GetMapping("/cursos/{seccionCursoId}/resumen")
    public AsistenciaDto.ResumenCurso resumen(@PathVariable Long seccionCursoId,
                                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                              @RequestHeader(value = USUARIO, required = false) Long usuarioId) {
        return asistenciaService.resumenCurso(usuarioId, seccionCursoId, desde, hasta);
    }

    /**
     * Endpoint GET /api/asistencia/mia — Devuelve mía. La lógica está en asistenciaService.mia().
     */
    @GetMapping("/mia")
    public AsistenciaDto.DelAlumno mia(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                       @RequestHeader(value = USUARIO, required = false) Long usuarioId) {
        return asistenciaService.mia(usuarioId, desde, hasta);
    }

    /**
     * Endpoint GET /api/asistencia/estudiantes/{estudianteId} — Devuelve del alumno. La lógica está en
     * asistenciaService.delAlumno().
     */
    @GetMapping("/estudiantes/{estudianteId}")
    public AsistenciaDto.DelAlumno delAlumno(@PathVariable Long estudianteId,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                             @RequestHeader(value = USUARIO, required = false) Long usuarioId) {
        return asistenciaService.delAlumno(usuarioId, estudianteId, desde, hasta);
    }
}
