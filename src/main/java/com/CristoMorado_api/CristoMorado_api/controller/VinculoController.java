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

    /**
     * Constructor: Spring entrega (inyecta) automáticamente las dependencias que usa esta clase.
     */
    public VinculoController(VinculoService vinculoService) {
        this.vinculoService = vinculoService;
    }

    /**
     * Endpoint GET /api/vinculos — Devuelve la lista de los vínculos entre padres e hijos. La lógica
     * está en vinculoService.listar().
     */
    @GetMapping
    public List<VinculoResponse> listar() {
        return vinculoService.listar();
    }

    /**
     * Endpoint POST /api/vinculos — Crea un nuevo registro (el vínculo padre-hijo) con los datos
     * recibidos. La lógica está en vinculoService.crear().
     */
    @PostMapping
    public ResponseEntity<VinculoResponse> crear(@RequestBody VinculoRequest request) {
        return ResponseEntity.ok(vinculoService.crear(request));
    }

    /**
     * Endpoint PUT /api/vinculos/{id} — Actualiza los datos del vínculo padre-hijo indicado por su id.
     * La lógica está en vinculoService.actualizar().
     */
    @PutMapping("/{id}")
    public VinculoResponse actualizar(@PathVariable Long id, @RequestBody VinculoRequest request) {
        return vinculoService.actualizar(id, request);
    }

    /**
     * Endpoint DELETE /api/vinculos/{id} — Elimina el vínculo padre-hijo indicado por su id. La lógica
     * está en vinculoService.eliminar(). Si todo sale bien responde 204 (sin contenido).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        vinculoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
