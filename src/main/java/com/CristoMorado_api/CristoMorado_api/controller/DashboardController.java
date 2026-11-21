package com.CristoMorado_api.CristoMorado_api.controller;

import com.CristoMorado_api.CristoMorado_api.dto.ActividadReciente;
import com.CristoMorado_api.CristoMorado_api.dto.DashboardStats;
import com.CristoMorado_api.CristoMorado_api.services.DashboardService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "${app.cors.origenes}")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/stats")
    public DashboardStats stats() {
        return dashboardService.obtenerStats();
    }

    @GetMapping("/actividad-reciente")
    public List<ActividadReciente> actividadReciente() {
        return dashboardService.obtenerActividadReciente();
    }
}
