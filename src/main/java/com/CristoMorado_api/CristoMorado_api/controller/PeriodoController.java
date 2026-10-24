package com.CristoMorado_api.CristoMorado_api.controller;

import com.CristoMorado_api.CristoMorado_api.dto.PeriodoDto;
import com.CristoMorado_api.CristoMorado_api.services.PeriodoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Académico > Periodos. Crear, editar y activar exigen el permiso ACADEMICO (PermisoInterceptor). */
@RestController
@RequestMapping("/api/periodos")
@CrossOrigin(origins = "${app.cors.origenes}")
public class PeriodoController {

    private final PeriodoService periodoService;

    public PeriodoController(PeriodoService periodoService) {
        this.periodoService = periodoService;
    }

    @GetMapping
    public List<PeriodoDto.Response> listar() {
        return periodoService.listar();
    }

    @PostMapping
    public ResponseEntity<PeriodoDto.Response> crear(@RequestBody PeriodoDto.Request request) {
        return ResponseEntity.ok(periodoService.crear(request));
    }

    @PutMapping("/{id}")
    public PeriodoDto.Response actualizar(@PathVariable Long id, @RequestBody PeriodoDto.Request request) {
        return periodoService.actualizar(id, request);
    }

    @PatchMapping("/{id}/activar")
    public PeriodoDto.Response activar(@PathVariable Long id) {
        return periodoService.activar(id);
    }
}
