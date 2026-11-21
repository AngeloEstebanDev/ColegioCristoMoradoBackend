package com.CristoMorado_api.CristoMorado_api.services;

import com.CristoMorado_api.CristoMorado_api.dto.ActividadReciente;
import com.CristoMorado_api.CristoMorado_api.dto.DashboardStats;
import com.CristoMorado_api.CristoMorado_api.repository.EstudianteRepository;
import com.CristoMorado_api.CristoMorado_api.repository.PadreFamiliaRepository;
import com.CristoMorado_api.CristoMorado_api.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Servicio del panel de inicio del administrador (estadísticas y actividad reciente). Aquí está la
 * lógica de negocio: valida los datos, aplica las reglas del colegio y usa los repositorios para leer
 * o guardar en la base de datos. Los controladores llaman a estos métodos.
 */
@Service
public class DashboardService {

    private final UsuarioRepository usuarioRepository;
    private final EstudianteRepository estudianteRepository;
    private final PadreFamiliaRepository padreFamiliaRepository;
    private final ActividadLogService actividadLogService;

    /**
     * Constructor: Spring entrega (inyecta) automáticamente las dependencias que usa esta clase.
     */
    public DashboardService(UsuarioRepository usuarioRepository,
                             EstudianteRepository estudianteRepository,
                             PadreFamiliaRepository padreFamiliaRepository,
                             ActividadLogService actividadLogService) {
        this.usuarioRepository = usuarioRepository;
        this.estudianteRepository = estudianteRepository;
        this.padreFamiliaRepository = padreFamiliaRepository;
        this.actividadLogService = actividadLogService;
    }

    /**
     * Obtiene stats. Solo lee datos (transacción de solo lectura).
     */
    @Transactional(readOnly = true)
    public DashboardStats obtenerStats() {
        long usuariosActivos = usuarioRepository.countByEstadoTrue();
        long totalEstudiantes = estudianteRepository.count();
        long totalPadres = padreFamiliaRepository.count();
        long totalDocentes = usuarioRepository.countByRolIgnoreCase("docente");
        return new DashboardStats(usuariosActivos, totalEstudiantes, totalPadres, totalDocentes);
    }

    /** Últimas 20 acciones del sistema (inicios de sesión, registros, ediciones, eliminaciones...). */
    @Transactional(readOnly = true)
    public List<ActividadReciente> obtenerActividadReciente() {
        return actividadLogService.recientes().stream()
                .map(a -> new ActividadReciente(a.getFecha(), a.getUsuario(), a.getAccion(), a.getModulo(), a.getEstado()))
                .toList();
    }
}
