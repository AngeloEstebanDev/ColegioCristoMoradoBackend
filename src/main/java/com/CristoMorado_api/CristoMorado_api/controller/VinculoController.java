package com.CristoMorado_api.CristoMorado_api.controller;

import com.CristoMorado_api.CristoMorado_api.dto.VinculoRequest;
import com.CristoMorado_api.CristoMorado_api.dto.VinculoResponse;
import com.CristoMorado_api.CristoMorado_api.services.VinculoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Vínculo Padre-Hijo. POST/PUT/DELETE exigen el permiso ACADEMICO (PermisoInterceptor). */
@RestController
@RequestMapping("/api/vinculos")
@CrossOrigin(origins = "${app.cors.origenes}")
public class VinculoController {

    private final VinculoService vinculoService;

    public VinculoController(VinculoService vinculoService) {
        this.vinculoService = vinculoService;
    }

    @GetMapping
    public List<VinculoResponse> listar() {
        return vinculoService.listar();
    }

    @PostMapping
    public ResponseEntity<VinculoResponse> crear(@RequestBody VinculoRequest request) {
        return ResponseEntity.ok(vinculoService.crear(request));
    }

    @PutMapping("/{id}")
    public VinculoResponse actualizar(@PathVariable Long id, @RequestBody VinculoRequest request) {
        return vinculoService.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        vinculoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
