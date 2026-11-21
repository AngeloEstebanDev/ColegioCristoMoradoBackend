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

    /**
     * Constructor: Spring entrega (inyecta) automáticamente las dependencias que usa esta clase.
     */
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

    /**
     * Endpoint GET /api/secciones — Devuelve la lista de los grados, secciones, cursos por sección y
     * horarios. La lógica está en seccionService.listar().
     */
    @GetMapping("/api/secciones")
    public List<SeccionResponse> listar(@RequestParam(required = false) String grado) {
        return seccionService.listar(grado);
    }

    /**
     * Endpoint GET /api/secciones/{id} — Obtiene la sección indicado por su id. La lógica está en
     * seccionService.obtener().
     */
    @GetMapping("/api/secciones/{id}")
    public SeccionResponse obtener(@PathVariable Long id) {
        return seccionService.obtener(id);
    }

    /**
     * Endpoint POST /api/secciones — Crea un nuevo registro (la sección) con los datos recibidos. La
     * lógica está en seccionService.crear().
     */
    @PostMapping("/api/secciones")
    public ResponseEntity<SeccionResponse> crear(@RequestBody SeccionRequest request) {
        return ResponseEntity.ok(seccionService.crear(request));
    }

    /**
     * Endpoint PUT /api/secciones/{id} — Actualiza los datos de la sección indicado por su id. La
     * lógica está en seccionService.actualizar().
     */
    @PutMapping("/api/secciones/{id}")
    public SeccionResponse actualizar(@PathVariable Long id, @RequestBody SeccionRequest request) {
        return seccionService.actualizar(id, request);
    }

    /**
     * Endpoint PATCH /api/secciones/{id}/estado — Cambia estado. La lógica está en
     * seccionService.cambiarEstado().
     */
    @PatchMapping("/api/secciones/{id}/estado")
    public SeccionResponse cambiarEstado(@PathVariable Long id, @RequestBody EstadoRequest request) {
        return seccionService.cambiarEstado(id, estado(request));
    }

    /**
     * Endpoint PATCH /api/secciones/{id}/tutor — Cambia tutor. La lógica está en
     * seccionService.cambiarTutor().
     */
    @PatchMapping("/api/secciones/{id}/tutor")
    public SeccionResponse cambiarTutor(@PathVariable Long id, @RequestBody SeccionAccionRequest request) {
        return seccionService.cambiarTutor(id, request == null ? null : request.docenteId());
    }

    /**
     * Endpoint POST /api/secciones/{id}/alumnos — Devuelve matricular. La lógica está en
     * seccionService.matricular().
     */
    @PostMapping("/api/secciones/{id}/alumnos")
    public SeccionResponse matricular(@PathVariable Long id, @RequestBody SeccionAccionRequest request) {
        return seccionService.matricular(id, request == null ? null : request.estudianteId());
    }

    /**
     * Endpoint DELETE /api/secciones/{id}/alumnos/{estudianteId} — Retira los grados, secciones,
     * cursos por sección y horarios. La lógica está en seccionService.retirar().
     */
    @DeleteMapping("/api/secciones/{id}/alumnos/{estudianteId}")
    public SeccionResponse retirar(@PathVariable Long id, @PathVariable Long estudianteId) {
        return seccionService.retirar(id, estudianteId);
    }

    /**
     * Endpoint POST /api/secciones/{id}/cursos — Agrega curso. La lógica está en
     * seccionService.agregarCurso(). Si los datos no son válidos, responde con error 400 y un mensaje
     * claro para el usuario.
     */
    @PostMapping("/api/secciones/{id}/cursos")
    public SeccionResponse agregarCurso(@PathVariable Long id, @RequestBody SeccionAccionRequest request) {
        if (request == null) throw new DatosInvalidosException("Selecciona el curso.");
        return seccionService.agregarCurso(id, request.cursoId(), request.docenteId(), request.descripcion());
    }

    /**
     * Endpoint PATCH /api/secciones/{id}/cursos/{seccionCursoId}/docente — Asigna docente. La lógica
     * está en seccionService.asignarDocente().
     */
    @PatchMapping("/api/secciones/{id}/cursos/{seccionCursoId}/docente")
    public SeccionResponse asignarDocente(@PathVariable Long id, @PathVariable Long seccionCursoId,
                                          @RequestBody SeccionAccionRequest request) {
        return seccionService.asignarDocente(id, seccionCursoId, request == null ? null : request.docenteId());
    }

    /**
     * Endpoint PATCH /api/secciones/{id}/cursos/{seccionCursoId}/descripcion — Edita descripción. La
     * lógica está en seccionService.editarDescripcion().
     */
    @PatchMapping("/api/secciones/{id}/cursos/{seccionCursoId}/descripcion")
    public SeccionResponse editarDescripcion(@PathVariable Long id, @PathVariable Long seccionCursoId,
                                             @RequestBody SeccionAccionRequest request) {
        return seccionService.editarDescripcion(id, seccionCursoId, request == null ? null : request.descripcion());
    }

    /**
     * Endpoint PATCH /api/secciones/{id}/cursos/{seccionCursoId}/estado — Cambia estado curso. La
     * lógica está en seccionService.cambiarEstadoCurso().
     */
    @PatchMapping("/api/secciones/{id}/cursos/{seccionCursoId}/estado")
    public SeccionResponse cambiarEstadoCurso(@PathVariable Long id, @PathVariable Long seccionCursoId,
                                              @RequestBody EstadoRequest request) {
        return seccionService.cambiarEstadoCurso(id, seccionCursoId, estado(request));
    }

    /**
     * Devuelve estado (verdadero o falso). Si los datos no son válidos, responde con error 400 y un
     * mensaje claro para el usuario.
     */
    private static boolean estado(EstadoRequest request) {
        if (request == null || request.estado() == null) throw new DatosInvalidosException("Indica el nuevo estado.");
        return request.estado();
    }
}
