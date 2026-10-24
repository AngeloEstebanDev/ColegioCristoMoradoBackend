package com.CristoMorado_api.CristoMorado_api.controller;

import com.CristoMorado_api.CristoMorado_api.dto.EstudianteRequest;
import com.CristoMorado_api.CristoMorado_api.dto.EstudianteResponse;
import com.CristoMorado_api.CristoMorado_api.services.EstudianteService;
import com.CristoMorado_api.CristoMorado_api.dto.EstadoRequest;
import com.CristoMorado_api.CristoMorado_api.exception.DatosInvalidosException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/estudiantes")
@CrossOrigin(origins = "${app.cors.origenes}")
public class EstudianteController {

    private final EstudianteService estudianteService;

    public EstudianteController(EstudianteService estudianteService) {
        this.estudianteService = estudianteService;
    }

    @GetMapping
    public List<EstudianteResponse> listar() {
        return estudianteService.listar();
    }

    @GetMapping("/{id}")
    public EstudianteResponse obtener(@PathVariable Long id) {
        return estudianteService.obtener(id);
    }

    @PostMapping
    public ResponseEntity<EstudianteResponse> crear(@RequestBody EstudianteRequest request) {
        return ResponseEntity.ok(estudianteService.crear(request));
    }

    @PutMapping("/{id}")
    public EstudianteResponse actualizar(@PathVariable Long id, @RequestBody EstudianteRequest request) {
        return estudianteService.actualizar(id, request);
    }

    /** Activa o desactiva la cuenta: { "estado": true | false } */
    @PatchMapping("/{id}/estado")
    public EstudianteResponse cambiarEstado(@PathVariable Long id, @RequestBody EstadoRequest request) {
        if (request == null || request.estado() == null) {
            throw new DatosInvalidosException("Indica el nuevo estado de la cuenta.");
        }
        return estudianteService.cambiarEstado(id, request.estado());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        estudianteService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
