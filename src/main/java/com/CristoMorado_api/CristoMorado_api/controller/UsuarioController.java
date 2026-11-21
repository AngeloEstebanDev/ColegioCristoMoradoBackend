package com.CristoMorado_api.CristoMorado_api.controller;

import com.CristoMorado_api.CristoMorado_api.dto.EstadoRequest;
import com.CristoMorado_api.CristoMorado_api.dto.UsuarioRequest;
import com.CristoMorado_api.CristoMorado_api.dto.UsuarioResponse;
import com.CristoMorado_api.CristoMorado_api.services.UsuarioService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = "${app.cors.origenes}")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public List<UsuarioResponse> listar() {
        return usuarioService.listar();
    }

    @GetMapping("/{id}")
    public UsuarioResponse obtener(@PathVariable Long id) {
        return usuarioService.obtener(id);
    }

    @PostMapping
    public ResponseEntity<UsuarioResponse> crear(@RequestBody UsuarioRequest request,
                                                 @RequestParam(required = false) Long actorId) {
        return ResponseEntity.ok(usuarioService.crear(request, actorId));
    }

    @PutMapping("/{id}")
    public UsuarioResponse actualizar(@PathVariable Long id, @RequestBody UsuarioRequest request,
                                      @RequestParam(required = false) Long actorId) {
        return usuarioService.actualizar(id, request, actorId);
    }

    @PatchMapping("/{id}/estado")
    public UsuarioResponse cambiarEstado(@PathVariable Long id, @RequestBody EstadoRequest request,
                                         @RequestParam(required = false) Long actorId) {
        return usuarioService.cambiarEstado(id, Boolean.TRUE.equals(request.estado()), actorId);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id, @RequestParam(required = false) Long actorId) {
        usuarioService.eliminar(id, actorId);
        return ResponseEntity.noContent().build();
    }
}
