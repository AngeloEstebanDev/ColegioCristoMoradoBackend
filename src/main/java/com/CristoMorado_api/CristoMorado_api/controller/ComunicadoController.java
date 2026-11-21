package com.CristoMorado_api.CristoMorado_api.controller;

import com.CristoMorado_api.CristoMorado_api.dto.ComunicadoRequest;
import com.CristoMorado_api.CristoMorado_api.dto.ComunicadoResponse;
import com.CristoMorado_api.CristoMorado_api.services.ComunicadoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Comunicados. Igual que /api/perfil, el frontend envía el id del
 * usuario logeado (?usuarioId=) para saber qué puede ver o hacer.
 */
@RestController
@RequestMapping("/api/comunicados")
@CrossOrigin(origins = "${app.cors.origenes}")
public class ComunicadoController {

    private final ComunicadoService comunicadoService;

    /**
     * Constructor: Spring entrega (inyecta) automáticamente las dependencias que usa esta clase.
     */
    public ComunicadoController(ComunicadoService comunicadoService) {
        this.comunicadoService = comunicadoService;
    }

    /**
     * Endpoint GET /api/comunicados — Devuelve la lista de los comunicados institucionales. La lógica
     * está en comunicadoService.listar().
     */
    @GetMapping
    public List<ComunicadoResponse> listar(@RequestParam Long usuarioId) {
        return comunicadoService.listar(usuarioId);
    }

    /** Secciones a las que el usuario puede dirigir un comunicado (docente: donde enseña). */
    @GetMapping("/secciones")
    public List<java.util.Map<String, Object>> secciones(@RequestParam Long usuarioId) {
        return comunicadoService.seccionesParaPublicar(usuarioId);
    }

    /**
     * Endpoint GET /api/comunicados/{id} — Obtiene el comunicado indicado por su id. La lógica está en
     * comunicadoService.obtener().
     */
    @GetMapping("/{id}")
    public ComunicadoResponse obtener(@PathVariable Long id, @RequestParam Long usuarioId) {
        return comunicadoService.obtener(id, usuarioId);
    }

    /**
     * Endpoint POST /api/comunicados — Crea un nuevo registro (el comunicado) con los datos recibidos.
     * La lógica está en comunicadoService.crear().
     */
    @PostMapping
    public ResponseEntity<ComunicadoResponse> crear(@RequestParam Long usuarioId,
                                                    @RequestBody ComunicadoRequest request) {
        return ResponseEntity.ok(comunicadoService.crear(usuarioId, request));
    }

    /**
     * Endpoint PUT /api/comunicados/{id} — Actualiza los datos del comunicado indicado por su id. La
     * lógica está en comunicadoService.actualizar().
     */
    @PutMapping("/{id}")
    public ComunicadoResponse actualizar(@PathVariable Long id, @RequestParam Long usuarioId,
                                         @RequestBody ComunicadoRequest request) {
        return comunicadoService.actualizar(id, usuarioId, request);
    }

    /**
     * Endpoint DELETE /api/comunicados/{id} — Elimina el comunicado indicado por su id. La lógica está
     * en comunicadoService.eliminar(). Si todo sale bien responde 204 (sin contenido).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id, @RequestParam Long usuarioId) {
        comunicadoService.eliminar(id, usuarioId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Endpoint POST /api/comunicados/{id}/leido — Marca leido. La lógica está en
     * comunicadoService.marcarLeido(). Si todo sale bien responde 204 (sin contenido).
     */
    @PostMapping("/{id}/leido")
    public ResponseEntity<Void> marcarLeido(@PathVariable Long id, @RequestParam Long usuarioId) {
        comunicadoService.marcarLeido(id, usuarioId);
        return ResponseEntity.noContent().build();
    }
}
