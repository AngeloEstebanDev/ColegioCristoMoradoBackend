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

    public CursoController(CursoService cursoService) {
        this.cursoService = cursoService;
    }

    @GetMapping
    public List<CursoResponse> listar() {
        return cursoService.listar();
    }

    @PostMapping
    public ResponseEntity<CursoResponse> crear(@RequestBody CursoRequest request) {
        return ResponseEntity.ok(cursoService.crear(request));
    }

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
