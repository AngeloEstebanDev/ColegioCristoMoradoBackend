package com.CristoMorado_api.CristoMorado_api.controller;

import com.CristoMorado_api.CristoMorado_api.dto.PadreFamiliaRequest;
import com.CristoMorado_api.CristoMorado_api.dto.PadreFamiliaResponse;
import com.CristoMorado_api.CristoMorado_api.services.PadreFamiliaService;
import com.CristoMorado_api.CristoMorado_api.dto.EstadoRequest;
import com.CristoMorado_api.CristoMorado_api.exception.DatosInvalidosException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST de los padres de familia. Recibe las peticiones HTTP del frontend en las rutas que
 * empiezan con /api/padres, revisa los datos básicos y llama al servicio que tiene la lógica. Lo que
 * devuelve cada método se convierte automáticamente a JSON.
 */
@RestController
@RequestMapping("/api/padres")
@CrossOrigin(origins = "${app.cors.origenes}")
public class PadreFamiliaController {

    private final PadreFamiliaService padreFamiliaService;

    /**
     * Constructor: Spring entrega (inyecta) automáticamente las dependencias que usa esta clase.
     */
    public PadreFamiliaController(PadreFamiliaService padreFamiliaService) {
        this.padreFamiliaService = padreFamiliaService;
    }

    /**
     * Endpoint GET /api/padres — Devuelve la lista de los padres de familia. La lógica está en
     * padreFamiliaService.listar().
     */
    @GetMapping
    public List<PadreFamiliaResponse> listar() {
        return padreFamiliaService.listar();
    }

    /**
     * Endpoint GET /api/padres/{id} — Obtiene el padre de familia indicado por su id. La lógica está
     * en padreFamiliaService.obtener().
     */
    @GetMapping("/{id}")
    public PadreFamiliaResponse obtener(@PathVariable Long id) {
        return padreFamiliaService.obtener(id);
    }

    /**
     * Endpoint POST /api/padres — Crea un nuevo registro (el padre de familia) con los datos
     * recibidos. La lógica está en padreFamiliaService.crear().
     */
    @PostMapping
    public ResponseEntity<PadreFamiliaResponse> crear(@RequestBody PadreFamiliaRequest request) {
        return ResponseEntity.ok(padreFamiliaService.crear(request));
    }

    /**
     * Endpoint PUT /api/padres/{id} — Actualiza los datos del padre de familia indicado por su id. La
     * lógica está en padreFamiliaService.actualizar().
     */
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

    /**
     * Endpoint DELETE /api/padres/{id} — Elimina el padre de familia indicado por su id. La lógica
     * está en padreFamiliaService.eliminar(). Si todo sale bien responde 204 (sin contenido).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        padreFamiliaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
