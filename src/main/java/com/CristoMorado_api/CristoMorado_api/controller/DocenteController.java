package com.CristoMorado_api.CristoMorado_api.controller;

import com.CristoMorado_api.CristoMorado_api.dto.DocenteRequest;
import com.CristoMorado_api.CristoMorado_api.dto.DocenteResponse;
import com.CristoMorado_api.CristoMorado_api.services.DocenteService;
import com.CristoMorado_api.CristoMorado_api.dto.EstadoRequest;
import com.CristoMorado_api.CristoMorado_api.exception.DatosInvalidosException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST de los docentes. Recibe las peticiones HTTP del frontend en las rutas que empiezan
 * con /api/docentes, revisa los datos básicos y llama al servicio que tiene la lógica. Lo que devuelve
 * cada método se convierte automáticamente a JSON.
 */
@RestController
@RequestMapping("/api/docentes")
@CrossOrigin(origins = "${app.cors.origenes}")
public class DocenteController {

    private final DocenteService docenteService;

    /**
     * Constructor: Spring entrega (inyecta) automáticamente las dependencias que usa esta clase.
     */
    public DocenteController(DocenteService docenteService) {
        this.docenteService = docenteService;
    }

    /**
     * Endpoint GET /api/docentes — Devuelve la lista de los docentes. La lógica está en
     * docenteService.listar().
     */
    @GetMapping
    public List<DocenteResponse> listar() {
        return docenteService.listar();
    }

    /**
     * Endpoint GET /api/docentes/{id} — Obtiene el docente indicado por su id. La lógica está en
     * docenteService.obtener().
     */
    @GetMapping("/{id}")
    public DocenteResponse obtener(@PathVariable Long id) {
        return docenteService.obtener(id);
    }

    /**
     * Endpoint POST /api/docentes — Crea un nuevo registro (el docente) con los datos recibidos. La
     * lógica está en docenteService.crear().
     */
    @PostMapping
    public ResponseEntity<DocenteResponse> crear(@RequestBody DocenteRequest request) {
        return ResponseEntity.ok(docenteService.crear(request));
    }

    /**
     * Endpoint PUT /api/docentes/{id} — Actualiza los datos del docente indicado por su id. La lógica
     * está en docenteService.actualizar().
     */
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

    /**
     * Endpoint DELETE /api/docentes/{id} — Elimina el docente indicado por su id. La lógica está en
     * docenteService.eliminar(). Si todo sale bien responde 204 (sin contenido).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        docenteService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
