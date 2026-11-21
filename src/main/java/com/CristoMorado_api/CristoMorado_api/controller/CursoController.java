package com.CristoMorado_api.CristoMorado_api.controller;

import com.CristoMorado_api.CristoMorado_api.dto.CursoRequest;
import com.CristoMorado_api.CristoMorado_api.dto.CursoResponse;
import com.CristoMorado_api.CristoMorado_api.services.CursoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Catálogo de Cursos. POST/PUT/DELETE exigen el permiso ACADEMICO (PermisoInterceptor). */
@RestController
@RequestMapping("/api/cursos")
@CrossOrigin(origins = "${app.cors.origenes}")
public class CursoController {

    private final CursoService cursoService;

    /**
     * Constructor: Spring entrega (inyecta) automáticamente las dependencias que usa esta clase.
     */
    public CursoController(CursoService cursoService) {
        this.cursoService = cursoService;
    }

    /**
     * Endpoint GET /api/cursos — Devuelve la lista del catálogo de cursos. La lógica está en
     * cursoService.listar().
     */
    @GetMapping
    public List<CursoResponse> listar() {
        return cursoService.listar();
    }

    /**
     * Endpoint POST /api/cursos — Crea un nuevo registro (el curso) con los datos recibidos. La lógica
     * está en cursoService.crear().
     */
    @PostMapping
    public ResponseEntity<CursoResponse> crear(@RequestBody CursoRequest request) {
        return ResponseEntity.ok(cursoService.crear(request));
    }

    /**
     * Endpoint PUT /api/cursos/{id} — Actualiza los datos del curso indicado por su id. La lógica está
     * en cursoService.actualizar().
     */
    @PutMapping("/{id}")
    public CursoResponse actualizar(@PathVariable Long id, @RequestBody CursoRequest request) {
        return cursoService.actualizar(id, request);
    }

    /** Responde cuántas secciones conservan el curso (0 = se borró por completo). */
    @DeleteMapping("/{id}")
    public Map<String, Long> eliminar(@PathVariable Long id) {
        return Map.of("seccionesQueLoConservan", cursoService.eliminar(id));
    }
}
