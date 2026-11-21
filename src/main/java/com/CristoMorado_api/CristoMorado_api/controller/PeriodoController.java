package com.CristoMorado_api.CristoMorado_api.controller;

import com.CristoMorado_api.CristoMorado_api.dto.PeriodoDto;
import com.CristoMorado_api.CristoMorado_api.services.PeriodoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Académico > Periodos. Crear y editar exigen el permiso ACADEMICO (PermisoInterceptor). */
@RestController
@RequestMapping("/api/periodos")
@CrossOrigin(origins = "${app.cors.origenes}")
public class PeriodoController {

    private final PeriodoService periodoService;

    /**
     * Constructor: Spring entrega (inyecta) automáticamente las dependencias que usa esta clase.
     */
    public PeriodoController(PeriodoService periodoService) {
        this.periodoService = periodoService;
    }

    /**
     * Endpoint GET /api/periodos — Devuelve la lista de los periodos académicos (bimestres). La lógica
     * está en periodoService.listar().
     */
    @GetMapping
    public List<PeriodoDto.Response> listar() {
        return periodoService.listar();
    }

    /**
     * Endpoint POST /api/periodos — Crea un nuevo registro (el periodo) con los datos recibidos. La
     * lógica está en periodoService.crear().
     */
    @PostMapping
    public ResponseEntity<PeriodoDto.Response> crear(@RequestBody PeriodoDto.Request request) {
        return ResponseEntity.ok(periodoService.crear(request));
    }

    /**
     * Endpoint PUT /api/periodos/{id} — Actualiza los datos del periodo indicado por su id. La lógica
     * está en periodoService.actualizar().
     */
    @PutMapping("/{id}")
    public PeriodoDto.Response actualizar(@PathVariable Long id, @RequestBody PeriodoDto.Request request) {
        return periodoService.actualizar(id, request);
    }
}
