package com.CristoMorado_api.CristoMorado_api.controller;

import com.CristoMorado_api.CristoMorado_api.dto.EstadoRequest;
import com.CristoMorado_api.CristoMorado_api.dto.UsuarioRequest;
import com.CristoMorado_api.CristoMorado_api.dto.UsuarioResponse;
import com.CristoMorado_api.CristoMorado_api.services.UsuarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST de las cuentas de usuario, roles y permisos. Recibe las peticiones HTTP del
 * frontend en las rutas que empiezan con /api/usuarios, revisa los datos básicos y llama al servicio
 * que tiene la lógica. Lo que devuelve cada método se convierte automáticamente a JSON.
 */
@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = "${app.cors.origenes}")
public class UsuarioController {

    private final UsuarioService usuarioService;

    /**
     * Constructor: Spring entrega (inyecta) automáticamente las dependencias que usa esta clase.
     */
    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    /**
     * Endpoint GET /api/usuarios — Devuelve la lista de las cuentas de usuario, roles y permisos. La
     * lógica está en usuarioService.listar().
     */
    @GetMapping
    public List<UsuarioResponse> listar() {
        return usuarioService.listar();
    }

    /**
     * Endpoint GET /api/usuarios/{id} — Obtiene la cuenta de usuario indicado por su id. La lógica
     * está en usuarioService.obtener().
     */
    @GetMapping("/{id}")
    public UsuarioResponse obtener(@PathVariable Long id) {
        return usuarioService.obtener(id);
    }

    /**
     * Endpoint POST /api/usuarios — Crea un nuevo registro (la cuenta de usuario) con los datos
     * recibidos. La lógica está en usuarioService.crear().
     */
    @PostMapping
    public ResponseEntity<UsuarioResponse> crear(@RequestBody UsuarioRequest request,
                                                 @RequestParam(required = false) Long actorId) {
        return ResponseEntity.ok(usuarioService.crear(request, actorId));
    }

    /**
     * Endpoint PUT /api/usuarios/{id} — Actualiza los datos de la cuenta de usuario indicado por su
     * id. La lógica está en usuarioService.actualizar().
     */
    @PutMapping("/{id}")
    public UsuarioResponse actualizar(@PathVariable Long id, @RequestBody UsuarioRequest request,
                                      @RequestParam(required = false) Long actorId) {
        return usuarioService.actualizar(id, request, actorId);
    }

    /**
     * Endpoint PATCH /api/usuarios/{id}/estado — Cambia estado. La lógica está en
     * usuarioService.cambiarEstado().
     */
    @PatchMapping("/{id}/estado")
    public UsuarioResponse cambiarEstado(@PathVariable Long id, @RequestBody EstadoRequest request,
                                         @RequestParam(required = false) Long actorId) {
        return usuarioService.cambiarEstado(id, Boolean.TRUE.equals(request.estado()), actorId);
    }

    /**
     * Endpoint DELETE /api/usuarios/{id} — Elimina la cuenta de usuario indicado por su id. La lógica
     * está en usuarioService.eliminar(). Si todo sale bien responde 204 (sin contenido).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id, @RequestParam(required = false) Long actorId) {
        usuarioService.eliminar(id, actorId);
        return ResponseEntity.noContent().build();
    }
}
