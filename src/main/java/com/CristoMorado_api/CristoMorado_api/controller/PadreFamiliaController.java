package com.CristoMorado_api.CristoMorado_api.controller;

import com.CristoMorado_api.CristoMorado_api.dto.PadreFamiliaRequest;
import com.CristoMorado_api.CristoMorado_api.dto.PadreFamiliaResponse;
import com.CristoMorado_api.CristoMorado_api.services.PadreFamiliaService;
import com.CristoMorado_api.CristoMorado_api.dto.EstadoRequest;
import com.CristoMorado_api.CristoMorado_api.exception.DatosInvalidosException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/padres")
@CrossOrigin(origins = "${app.cors.origenes}")
public class PadreFamiliaController {

    private final PadreFamiliaService padreFamiliaService;

    public PadreFamiliaController(PadreFamiliaService padreFamiliaService) {
        this.padreFamiliaService = padreFamiliaService;
    }

    @GetMapping
    public List<PadreFamiliaResponse> listar() {
        return padreFamiliaService.listar();
    }

    @GetMapping("/{id}")
    public PadreFamiliaResponse obtener(@PathVariable Long id) {
        return padreFamiliaService.obtener(id);
    }

    @PostMapping
    public ResponseEntity<PadreFamiliaResponse> crear(@RequestBody PadreFamiliaRequest request) {
        return ResponseEntity.ok(padreFamiliaService.crear(request));
    }

    @PutMapping("/{id}")
    public PadreFamiliaResponse actualizar(@PathVariable Long id, @RequestBody PadreFamiliaRequest request) {
        return padreFamiliaService.actualizar(id, request);
    }

    /** Activa o desactiva la cuenta: { "estado": true | false } */
    @PatchMapping("/{id}/estado")
    public PadreFamiliaResponse cambiarEstado(@PathVariable Long id, @RequestBody EstadoRequest request) {
        if (request == null || request.estado() == null) {
            throw new DatosInvalidosException("Indica el nuevo estado de la cuenta.");
        }
        return padreFamiliaService.cambiarEstado(id, request.estado());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        padreFamiliaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
