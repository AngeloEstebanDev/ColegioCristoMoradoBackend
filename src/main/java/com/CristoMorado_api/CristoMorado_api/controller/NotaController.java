package com.CristoMorado_api.CristoMorado_api.controller;

import com.CristoMorado_api.CristoMorado_api.dto.NotaDto;
import com.CristoMorado_api.CristoMorado_api.services.NotaService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Calificaciones (HU-16, 17, 19, 20, 21).
 *  Docente: registro de notas de su curso por periodo, evaluaciones, guardar notas e historial de cambios.
 *  Alumno/padre: boleta (notas detalladas y promedios) e historial académico.
 */
@RestController
@RequestMapping("/api/notas")
@CrossOrigin(origins = "${app.cors.origenes}")
public class NotaController {

    private static final String USUARIO = "X-Usuario-Id";

    private final NotaService notaService;

    /**
     * Constructor: Spring entrega (inyecta) automáticamente las dependencias que usa esta clase.
     */
    public NotaController(NotaService notaService) {
        this.notaService = notaService;
    }

    /**
     * Endpoint GET /api/notas/cursos/{seccionCursoId} — Devuelve registro. La lógica está en
     * notaService.registro().
     */
    @GetMapping("/cursos/{seccionCursoId}")
    public NotaDto.Registro registro(@PathVariable Long seccionCursoId, @RequestParam(required = false) Long periodoId,
                                     @RequestHeader(value = USUARIO, required = false) Long usuarioId) {
        return notaService.registro(usuarioId, seccionCursoId, periodoId);
    }

    /** Participación (promedio de tareas, cuestionarios y actividades) por alumno, con su detalle. */
    @GetMapping("/cursos/{seccionCursoId}/participacion")
    public NotaDto.Participacion participacion(@PathVariable Long seccionCursoId, @RequestParam(required = false) Long periodoId,
                                               @RequestHeader(value = USUARIO, required = false) Long usuarioId) {
        return notaService.participacion(usuarioId, seccionCursoId, periodoId);
    }

    /**
     * Endpoint POST /api/notas/cursos/{seccionCursoId}/evaluaciones — Crea evaluación. La lógica está
     * en notaService.crearEvaluacion().
     */
    @PostMapping("/cursos/{seccionCursoId}/evaluaciones")
    public NotaDto.Registro crearEvaluacion(@PathVariable Long seccionCursoId, @RequestBody NotaDto.EvaluacionRequest req,
                                            @RequestHeader(value = USUARIO, required = false) Long usuarioId) {
        return notaService.crearEvaluacion(usuarioId, seccionCursoId, req);
    }

    /**
     * Endpoint PUT /api/notas/evaluaciones/{id} — Actualiza evaluación. La lógica está en
     * notaService.actualizarEvaluacion().
     */
    @PutMapping("/evaluaciones/{id}")
    public NotaDto.Registro actualizarEvaluacion(@PathVariable Long id, @RequestBody NotaDto.EvaluacionRequest req,
                                                 @RequestHeader(value = USUARIO, required = false) Long usuarioId) {
        return notaService.actualizarEvaluacion(usuarioId, id, req);
    }

    /**
     * Endpoint DELETE /api/notas/evaluaciones/{id} — Elimina evaluación. La lógica está en
     * notaService.eliminarEvaluacion().
     */
    @DeleteMapping("/evaluaciones/{id}")
    public NotaDto.Registro eliminarEvaluacion(@PathVariable Long id,
                                               @RequestHeader(value = USUARIO, required = false) Long usuarioId) {
        return notaService.eliminarEvaluacion(usuarioId, id);
    }

    /**
     * Endpoint PUT /api/notas/cursos/{seccionCursoId}/notas — Guarda notas. La lógica está en
     * notaService.guardarNotas().
     */
    @PutMapping("/cursos/{seccionCursoId}/notas")
    public NotaDto.Registro guardarNotas(@PathVariable Long seccionCursoId, @RequestBody NotaDto.GuardarNotasRequest req,
                                         @RequestHeader(value = USUARIO, required = false) Long usuarioId) {
        return notaService.guardarNotas(usuarioId, seccionCursoId, req);
    }

    /**
     * Endpoint GET /api/notas/cursos/{seccionCursoId}/historial — Devuelve historial curso (como
     * lista). La lógica está en notaService.historialCurso().
     */
    @GetMapping("/cursos/{seccionCursoId}/historial")
    public List<NotaDto.CambioHistorial> historialCurso(@PathVariable Long seccionCursoId,
                                                        @RequestParam(required = false) Long periodoId,
                                                        @RequestHeader(value = USUARIO, required = false) Long usuarioId) {
        return notaService.historialCurso(usuarioId, seccionCursoId, periodoId);
    }

    // ---------------- Alumno / padre ----------------

    /**
     * Endpoint GET /api/notas/mias — Devuelve mias. La lógica está en notaService.miBoleta().
     */
    @GetMapping("/mias")
    public NotaDto.Boleta mias(@RequestParam(required = false) Integer anio,
                               @RequestHeader(value = USUARIO, required = false) Long usuarioId) {
        return notaService.miBoleta(usuarioId, anio);
    }

    /**
     * Endpoint GET /api/notas/mi-historial — Devuelve mi historial del usuario que inició sesión. La
     * lógica está en notaService.miHistorial().
     */
    @GetMapping("/mi-historial")
    public NotaDto.Historial miHistorial(@RequestHeader(value = USUARIO, required = false) Long usuarioId) {
        return notaService.miHistorial(usuarioId);
    }

    /**
     * Endpoint GET /api/notas/estudiantes/{estudianteId} — Devuelve boleta. La lógica está en
     * notaService.boleta().
     */
    @GetMapping("/estudiantes/{estudianteId}")
    public NotaDto.Boleta boleta(@PathVariable Long estudianteId, @RequestParam(required = false) Integer anio,
                                 @RequestHeader(value = USUARIO, required = false) Long usuarioId) {
        return notaService.boleta(usuarioId, estudianteId, anio);
    }

    /**
     * Endpoint GET /api/notas/estudiantes/{estudianteId}/historial — Devuelve historial. La lógica
     * está en notaService.historial().
     */
    @GetMapping("/estudiantes/{estudianteId}/historial")
    public NotaDto.Historial historial(@PathVariable Long estudianteId,
                                       @RequestHeader(value = USUARIO, required = false) Long usuarioId) {
        return notaService.historial(usuarioId, estudianteId);
    }
}
