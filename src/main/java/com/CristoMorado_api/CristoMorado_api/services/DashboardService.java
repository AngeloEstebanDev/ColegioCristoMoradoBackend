package com.CristoMorado_api.CristoMorado_api.services;

import com.CristoMorado_api.CristoMorado_api.dto.ActividadReciente;
import com.CristoMorado_api.CristoMorado_api.dto.DashboardStats;
import com.CristoMorado_api.CristoMorado_api.entity.Estudiante;
import com.CristoMorado_api.CristoMorado_api.entity.PadreFamilia;
import com.CristoMorado_api.CristoMorado_api.entity.Usuario;
import com.CristoMorado_api.CristoMorado_api.repository.EstudianteRepository;
import com.CristoMorado_api.CristoMorado_api.repository.PadreFamiliaRepository;
import com.CristoMorado_api.CristoMorado_api.repository.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class DashboardService {

    private final UsuarioRepository usuarioRepository;
    private final EstudianteRepository estudianteRepository;
    private final PadreFamiliaRepository padreFamiliaRepository;

    public DashboardService(UsuarioRepository usuarioRepository,
                             EstudianteRepository estudianteRepository,
                             PadreFamiliaRepository padreFamiliaRepository) {
        this.usuarioRepository = usuarioRepository;
        this.estudianteRepository = estudianteRepository;
        this.padreFamiliaRepository = padreFamiliaRepository;
    }

    @Transactional(readOnly = true)
    public DashboardStats obtenerStats() {
        long usuariosActivos = usuarioRepository.countByEstadoTrue();
        long totalEstudiantes = estudianteRepository.count();
        long totalPadres = padreFamiliaRepository.count();
        long totalDocentes = usuarioRepository.countByRolIgnoreCase("docente");
        return new DashboardStats(usuariosActivos, totalEstudiantes, totalPadres, totalDocentes);
    }

    @Transactional(readOnly = true)
    public List<ActividadReciente> obtenerActividadReciente() {
        List<ActividadReciente> actividad = new ArrayList<>();

        for (Usuario u : usuarioRepository.findAll()) {
            actividad.add(new ActividadReciente(
                    u.getFechaRegistro(), u.getNombre(), "Nuevo usuario registrado",
                    "Usuarios y Roles", "Exitoso"));
        }
        for (Estudiante e : estudianteRepository.findAll()) {
            actividad.add(new ActividadReciente(
                    e.getFechaRegistro(), e.getNombreCompleto(), "Estudiante registrado",
                    "Estudiantes", "Exitoso"));
        }
        for (PadreFamilia p : padreFamiliaRepository.findAll()) {
            actividad.add(new ActividadReciente(
                    p.getFechaRegistro(), p.getNombreCompleto(), "Padre de familia registrado",
                    "Padres de Familia", "Exitoso"));
        }

        return actividad.stream()
                .sorted(Comparator.comparing(ActividadReciente::fecha).reversed())
                .limit(10)
                .toList();
    }
}
