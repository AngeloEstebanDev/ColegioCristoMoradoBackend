package com.CristoMorado_api.CristoMorado_api.controller;

import com.CristoMorado_api.CristoMorado_api.dto.*;
import com.CristoMorado_api.CristoMorado_api.dto.VerificacionRequests.CodigoRequest;
import com.CristoMorado_api.CristoMorado_api.dto.VerificacionRequests.CorreoRequest;
import com.CristoMorado_api.CristoMorado_api.services.PerfilService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * "Mi perfil" y "Configuración", compartidos por todos los roles. El
 * frontend los llama con el id del usuario logeado.
 */
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

    /** Paso 1 para vincular Gmail: envía el código al correo indicado. */
    @PostMapping("/{usuarioId}/correo/codigo")
    public CodigoEnviadoResponse enviarCodigoCorreo(@PathVariable Long usuarioId, @RequestBody CorreoRequest request) {
        return perfilService.enviarCodigoCorreo(usuarioId, request.correo());
    }

    /** Paso 2 para vincular Gmail: confirma el código y guarda el correo como verificado. */
    @PostMapping("/{usuarioId}/correo/verificar")
    public PerfilResponse verificarCorreo(@PathVariable Long usuarioId, @RequestBody CodigoRequest request) {
        perfilService.verificarCorreo(usuarioId, request.codigo());
        return perfilService.obtener(usuarioId);
    }

    /** Cambiar contraseña, paso 1: envía el código al Gmail verificado. */
    @PostMapping("/{usuarioId}/password/codigo")
    public CodigoEnviadoResponse enviarCodigoPassword(@PathVariable Long usuarioId) {
        return perfilService.enviarCodigoPassword(usuarioId);
    }

    /** Cambiar contraseña, paso 2: contraseña actual + nueva + código. */
    @PatchMapping("/{usuarioId}/password")
    public ResponseEntity<Void> cambiarPassword(@PathVariable Long usuarioId, @RequestBody CambiarPasswordRequest request) {
        perfilService.cambiarPassword(usuarioId, request);
        return ResponseEntity.noContent().build();
    }
}
