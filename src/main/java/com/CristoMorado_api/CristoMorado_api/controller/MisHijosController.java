package com.CristoMorado_api.CristoMorado_api.controller;

import com.CristoMorado_api.CristoMorado_api.entity.Estudiante;
import com.CristoMorado_api.CristoMorado_api.entity.PadreFamilia;
import com.CristoMorado_api.CristoMorado_api.services.AccesoService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Comparator;
import java.util.List;

/** Padre de familia: sus hijos vinculados (para elegir de quién ver notas, asistencia, tareas, citas). */
@RestController
@RequestMapping("/api/mis-hijos")
@CrossOrigin(origins = "${app.cors.origenes}")
public class MisHijosController {

    private final AccesoService accesoService;

    /**
     * Constructor: Spring entrega (inyecta) automáticamente las dependencias que usa esta clase.
     */
    public MisHijosController(AccesoService accesoService) {
        this.accesoService = accesoService;
    }

    /**
     * Estructura de datos Hijo (record): agrupa hijo en un solo objeto inmutable que viaja como JSON.
     */
    public record Hijo(Long id, String nombre, String grado, String seccion, String salon, String parentesco,
                       String estado, Long usuarioId) {}

    /**
     * Endpoint GET /api/mis-hijos — Devuelve la lista de los hijos del padre de familia que inició
     * sesión. La lógica está en accesoService.padreLogeado(), AccesoService.salon(). Solo lee datos
     * (transacción de solo lectura). Sabe quién hace la petición por la cabecera X-Usuario-Id que
     * envía el frontend.
     */
    @GetMapping
    @Transactional(readOnly = true)
    public List<Hijo> listar(@RequestHeader(value = "X-Usuario-Id", required = false) Long usuarioId) {
        PadreFamilia p = accesoService.padreLogeado(usuarioId);
        return p.getVinculosFamiliares().stream()
                .map(v -> {
                    Estudiante e = v.getEstudiante();
                    return new Hijo(e.getId(), e.getNombreCompleto(), e.getGrado(), e.getSeccion(),
                            AccesoService.salon(e), v.getParentesco(), e.getEstado(),
                            e.getUsuario() == null ? null : e.getUsuario().getId());
                })
                .sorted(Comparator.comparing(Hijo::nombre, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }
}
