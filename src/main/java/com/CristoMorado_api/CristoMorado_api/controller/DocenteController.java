package com.CristoMorado_api.CristoMorado_api.controller;

import com.CristoMorado_api.CristoMorado_api.dto.DocenteRequest;
import com.CristoMorado_api.CristoMorado_api.dto.DocenteResponse;
import com.CristoMorado_api.CristoMorado_api.services.DocenteService;
import com.CristoMorado_api.CristoMorado_api.dto.EstadoRequest;
import com.CristoMorado_api.CristoMorado_api.exception.DatosInvalidosException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/docentes")
@CrossOrigin(origins = "${app.cors.origenes}")
public class DocenteController {

    private final DocenteService docenteService;

    public DocenteController(DocenteService docenteService) {
        this.docenteService = docenteService;
    }

    @GetMapping
    public List<DocenteResponse> listar() {
        return docenteService.listar();
    }

    @GetMapping("/{id}")
    public DocenteResponse obtener(@PathVariable Long id) {
        return docenteService.obtener(id);
    }

    @PostMapping
    public ResponseEntity<DocenteResponse> crear(@RequestBody DocenteRequest request) {
        return ResponseEntity.ok(docenteService.crear(request));
    }

    @PutMapping("/{id}")
    public DocenteResponse actualizar(@PathVariable Long id, @RequestBody DocenteRequest request) {
        return docenteService.actualizar(id, request);
    }

    /** Activa o desactiva la cuenta: { "estado": true | false } */
    @PatchMapping("/{id}/estado")
    public DocenteResponse cambiarEstado(@PathVariable Long id, @RequestBody EstadoRequest request) {
        if (request == null || request.estado() == null) {
            throw new DatosInvalidosException("Indica el nuevo estado de la cuenta.");
        }
        return docenteService.cambiarEstado(id, request.estado());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        docenteService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
