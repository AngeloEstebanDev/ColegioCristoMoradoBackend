package com.CristoMorado_api.CristoMorado_api.controller;

import com.CristoMorado_api.CristoMorado_api.services.CorreoService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Diagnóstico del envío de correos. Ábrelo en el navegador para saber qué método está activo:
 * local: http://localhost:8080/api/correo/estado  ·  nube: https://(tu backend en Render)/api/correo/estado
 * No muestra claves ni URLs, solo el nombre del método.
 */
@RestController
@RequestMapping("/api/correo")
@CrossOrigin(origins = "${app.cors.origenes}")
public class CorreoController {

    private final CorreoService correoService;

    /**
     * Constructor: Spring entrega (inyecta) automáticamente las dependencias que usa esta clase.
     */
    public CorreoController(CorreoService correoService) {
        this.correoService = correoService;
    }

    /**
     * Endpoint GET /api/correo/estado — Devuelve el método de envío configurado:
     * APPS_SCRIPT, BREVO, SMTP, CONSOLA (solo local) o NINGUNO (vincular Gmail fallará).
     */
    @GetMapping("/estado")
    public Map<String, Object> estado() {
        String metodo = correoService.metodo();
        return Map.of(
                "metodo", metodo,
                "configurado", !"NINGUNO".equals(metodo),
                "modoConsola", correoService.isModoConsola());
    }
}
