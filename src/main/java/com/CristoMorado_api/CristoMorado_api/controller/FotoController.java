package com.CristoMorado_api.CristoMorado_api.controller;

import com.CristoMorado_api.CristoMorado_api.entity.Usuario;
import com.CristoMorado_api.CristoMorado_api.repository.UsuarioRepository;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Fotos de perfil para los avatares de todo el sistema (comunicados, mensajes, citas,
 * listas de usuarios, participantes...).
 *  - GET /api/fotos        -> índice {usuarioId: versión o ruta de avatar}, solo de quienes tienen foto.
 *  - GET /api/fotos/{id}   -> la imagen subida por el usuario (se guarda en caché del navegador).
 */
@RestController
@RequestMapping("/api/fotos")
@CrossOrigin(origins = "${app.cors.origenes}")
public class FotoController {

    private final UsuarioRepository usuarioRepository;

    public FotoController(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public Map<Long, String> indice() {
        Map<Long, String> indice = new LinkedHashMap<>();
        for (Object[] fila : usuarioRepository.indiceFotos()) {
            indice.put(((Number) fila[0]).longValue(), String.valueOf(fila[1]));
        }
        return indice;
    }

    @GetMapping("/{usuarioId}")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> foto(@PathVariable Long usuarioId) {
        String foto = usuarioRepository.findById(usuarioId).map(Usuario::getFoto).orElse(null);
        // Solo las fotos subidas (data URL "data:image/png;base64,...."); los avatares los sirve el frontend.
        if (foto == null || !foto.startsWith("data:image/") || !foto.contains(";base64,")) {
            return ResponseEntity.notFound().build();
        }
        int coma = foto.indexOf(";base64,");
        String tipo = foto.substring("data:".length(), coma);
        byte[] datos;
        try {
            datos = Base64.getDecoder().decode(foto.substring(coma + ";base64,".length()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(tipo))
                .cacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic())
                .body(datos);
    }
}
