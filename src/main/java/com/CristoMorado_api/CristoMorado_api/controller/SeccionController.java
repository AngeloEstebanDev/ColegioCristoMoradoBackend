package com.CristoMorado_api.CristoMorado_api.controller;

import com.CristoMorado_api.CristoMorado_api.dto.*;
import com.CristoMorado_api.CristoMorado_api.exception.DatosInvalidosException;
import com.CristoMorado_api.CristoMorado_api.services.HorarioService;
import com.CristoMorado_api.CristoMorado_api.services.SeccionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Académico > Grados y Secciones. Escribir exige el permiso ACADEMICO
 * (PermisoInterceptor: prefijos /api/secciones y /api/cursos).
 */
@RestController
@CrossOrigin(origins = "${app.cors.origenes}")
public class SeccionController {

    private final SeccionService seccionService;
    private final HorarioService horarioService;

    public SeccionController(SeccionService seccionService, HorarioService horarioService) {
        this.seccionService = seccionService;
        this.horarioService = horarioService;
    }

    /** Editor de horario: cuadro del turno (A = mañana, B = tarde) y sus bloques. */
    @GetMapping("/api/secciones/{id}/horario")
    public HorarioDto.Respuesta horario(@PathVariable Long id) {
        return horarioService.obtener(id);
    }

    /** Guarda (reemplaza) todos los bloques del horario. */
    @PutMapping("/api/secciones/{id}/horario")
    public HorarioDto.Respuesta guardarHorario(@PathVariable Long id, @RequestBody HorarioDto.GuardarRequest request) {
        return horarioService.guardar(id, request);
    }

    @GetMapping("/api/secciones")
    public List<SeccionResponse> listar(@RequestParam(required = false) String grado) {
        return seccionService.listar(grado);
    }

    @GetMapping("/api/secciones/{id}")
    public SeccionResponse obtener(@PathVariable Long id) {
        return seccionService.obtener(id);
    }

    @PostMapping("/api/secciones")
    public ResponseEntity<SeccionResponse> crear(@RequestBody SeccionRequest request) {
        return ResponseEntity.ok(seccionService.crear(request));
    }

    @PutMapping("/api/secciones/{id}")
    public SeccionResponse actualizar(@PathVariable Long id, @RequestBody SeccionRequest request) {
        return seccionService.actualizar(id, request);
    }

    @PatchMapping("/api/secciones/{id}/estado")
    public SeccionResponse cambiarEstado(@PathVariable Long id, @RequestBody EstadoRequest request) {
        return seccionService.cambiarEstado(id, estado(request));
    }

    @PatchMapping("/api/secciones/{id}/tutor")
    public SeccionResponse cambiarTutor(@PathVariable Long id, @RequestBody SeccionAccionRequest request) {
        return seccionService.cambiarTutor(id, request == null ? null : request.docenteId());
    }

    @PostMapping("/api/secciones/{id}/alumnos")
    public SeccionResponse matricular(@PathVariable Long id, @RequestBody SeccionAccionRequest request) {
        return seccionService.matricular(id, request == null ? null : request.estudianteId());
    }

    @DeleteMapping("/api/secciones/{id}/alumnos/{estudianteId}")
    public SeccionResponse retirar(@PathVariable Long id, @PathVariable Long estudianteId) {
        return seccionService.retirar(id, estudianteId);
    }

    @PostMapping("/api/secciones/{id}/cursos")
    public SeccionResponse agregarCurso(@PathVariable Long id, @RequestBody SeccionAccionRequest request) {
        if (request == null) throw new DatosInvalidosException("Selecciona el curso.");
        return seccionService.agregarCurso(id, request.cursoId(), request.docenteId(), request.descripcion());
    }

    @PatchMapping("/api/secciones/{id}/cursos/{seccionCursoId}/docente")
    public SeccionResponse asignarDocente(@PathVariable Long id, @PathVariable Long seccionCursoId,
                                          @RequestBody SeccionAccionRequest request) {
        return seccionService.asignarDocente(id, seccionCursoId, request == null ? null : request.docenteId());
    }

    @PatchMapping("/api/secciones/{id}/cursos/{seccionCursoId}/descripcion")
    public SeccionResponse editarDescripcion(@PathVariable Long id, @PathVariable Long seccionCursoId,
                                             @RequestBody SeccionAccionRequest request) {
        return seccionService.editarDescripcion(id, seccionCursoId, request == null ? null : request.descripcion());
    }

    @PatchMapping("/api/secciones/{id}/cursos/{seccionCursoId}/estado")
    public SeccionResponse cambiarEstadoCurso(@PathVariable Long id, @PathVariable Long seccionCursoId,
                                              @RequestBody EstadoRequest request) {
        return seccionService.cambiarEstadoCurso(id, seccionCursoId, estado(request));
    }

    private static boolean estado(EstadoRequest request) {
        if (request == null || request.estado() == null) throw new DatosInvalidosException("Indica el nuevo estado.");
        return request.estado();
    }
}
