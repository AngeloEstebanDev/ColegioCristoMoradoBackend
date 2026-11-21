package com.CristoMorado_api.CristoMorado_api.controller;

import com.CristoMorado_api.CristoMorado_api.dto.*;
import com.CristoMorado_api.CristoMorado_api.dto.VerificacionRequests.CodigoRequest;
import com.CristoMorado_api.CristoMorado_api.dto.VerificacionRequests.CorreoRequest;
import com.CristoMorado_api.CristoMorado_api.services.PerfilService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/perfil")
@CrossOrigin(origins = "${app.cors.origenes}")
public class PerfilController {

    private final PerfilService perfilService;

    public PerfilController(PerfilService perfilService) {
        this.perfilService = perfilService;
    }

    @GetMapping("/{usuarioId}")
    public PerfilResponse obtener(@PathVariable Long usuarioId) {
        return perfilService.obtener(usuarioId);
    }

    @PutMapping("/{usuarioId}")
    public PerfilResponse actualizar(@PathVariable Long usuarioId, @RequestBody PerfilUpdateRequest request) {
        return perfilService.actualizar(usuarioId, request);
    }

    @PostMapping("/{usuarioId}/correo/codigo")
    public CodigoEnviadoResponse enviarCodigoCorreo(@PathVariable Long usuarioId, @RequestBody CorreoRequest request) {
        return perfilService.enviarCodigoCorreo(usuarioId, request.correo());
    }

    @PostMapping("/{usuarioId}/correo/verificar")
    public PerfilResponse verificarCorreo(@PathVariable Long usuarioId, @RequestBody CodigoRequest request) {
        perfilService.verificarCorreo(usuarioId, request.codigo());
        return perfilService.obtener(usuarioId);
    }

    @PostMapping("/{usuarioId}/password/codigo")
    public CodigoEnviadoResponse enviarCodigoPassword(@PathVariable Long usuarioId) {
        return perfilService.enviarCodigoPassword(usuarioId);
    }

    @PatchMapping("/{usuarioId}/password")
    public ResponseEntity<Void> cambiarPassword(@PathVariable Long usuarioId, @RequestBody CambiarPasswordRequest request) {
        perfilService.cambiarPassword(usuarioId, request);
        return ResponseEntity.noContent().build();
    }
}
