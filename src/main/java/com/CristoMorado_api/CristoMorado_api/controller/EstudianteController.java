package com.CristoMorado_api.CristoMorado_api.controller;

import com.CristoMorado_api.CristoMorado_api.dto.EstudianteRequest;
import com.CristoMorado_api.CristoMorado_api.dto.EstudianteResponse;
import com.CristoMorado_api.CristoMorado_api.services.EstudianteService;
import com.CristoMorado_api.CristoMorado_api.dto.EstadoRequest;
import com.CristoMorado_api.CristoMorado_api.exception.DatosInvalidosException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST de los estudiantes. Recibe las peticiones HTTP del frontend en las rutas que
 * empiezan con /api/estudiantes, revisa los datos básicos y llama al servicio que tiene la lógica. Lo
 * que devuelve cada método se convierte automáticamente a JSON.
 */
@RestController
@RequestMapping("/api/estudiantes")
@CrossOrigin(origins = "${app.cors.origenes}")
public class EstudianteController {

    private final EstudianteService estudianteService;

    /**
     * Constructor: Spring entrega (inyecta) automáticamente las dependencias que usa esta clase.
     */
    public EstudianteController(EstudianteService estudianteService) {
        this.estudianteService = estudianteService;
    }

    /**
     * Endpoint GET /api/estudiantes — Devuelve la lista de los estudiantes. La lógica está en
     * estudianteService.listar().
     */
    @GetMapping
    public List<EstudianteResponse> listar() {
        return estudianteService.listar();
    }

    /**
     * Endpoint GET /api/estudiantes/{id} — Obtiene el estudiante indicado por su id. La lógica está en
     * estudianteService.obtener().
     */
    @GetMapping("/{id}")
    public EstudianteResponse obtener(@PathVariable Long id) {
        return estudianteService.obtener(id);
    }

    /**
     * Endpoint POST /api/estudiantes — Crea un nuevo registro (el estudiante) con los datos recibidos.
     * La lógica está en estudianteService.crear().
     */
    @PostMapping
    public ResponseEntity<EstudianteResponse> crear(@RequestBody EstudianteRequest request) {
        return ResponseEntity.ok(estudianteService.crear(request));
    }

    /**
     * Endpoint PUT /api/estudiantes/{id} — Actualiza los datos del estudiante indicado por su id. La
     * lógica está en estudianteService.actualizar().
     */
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

    /**
     * Endpoint DELETE /api/estudiantes/{id} — Elimina el estudiante indicado por su id. La lógica está
     * en estudianteService.eliminar(). Si todo sale bien responde 204 (sin contenido).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        estudianteService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
