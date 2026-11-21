package com.CristoMorado_api.CristoMorado_api.controller;

import com.CristoMorado_api.CristoMorado_api.dto.ActividadReciente;
import com.CristoMorado_api.CristoMorado_api.dto.DashboardStats;
import com.CristoMorado_api.CristoMorado_api.services.DashboardService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST del panel de inicio del administrador (estadísticas y actividad reciente). Recibe
 * las peticiones HTTP del frontend en las rutas que empiezan con /api/dashboard, revisa los datos
 * básicos y llama al servicio que tiene la lógica. Lo que devuelve cada método se convierte
 * automáticamente a JSON.
 */
@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "${app.cors.origenes}")
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * Constructor: Spring entrega (inyecta) automáticamente las dependencias que usa esta clase.
     */
    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * Endpoint GET /api/dashboard/stats — Devuelve stats. La lógica está en
     * dashboardService.obtenerStats().
     */
    @GetMapping("/stats")
    public DashboardStats stats() {
        return dashboardService.obtenerStats();
    }

    /**
     * Endpoint GET /api/dashboard/actividad-reciente — Devuelve actividad reciente (como lista). La
     * lógica está en dashboardService.obtenerActividadReciente().
     */
    @GetMapping("/actividad-reciente")
    public List<ActividadReciente> actividadReciente() {
        return dashboardService.obtenerActividadReciente();
    }
}
