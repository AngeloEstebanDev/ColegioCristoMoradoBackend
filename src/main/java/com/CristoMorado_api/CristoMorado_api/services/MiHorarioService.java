package com.CristoMorado_api.CristoMorado_api.services;

import com.CristoMorado_api.CristoMorado_api.dto.HorarioDto;
import com.CristoMorado_api.CristoMorado_api.dto.MiHorarioDto;
import com.CristoMorado_api.CristoMorado_api.entity.Estudiante;
import com.CristoMorado_api.CristoMorado_api.entity.Seccion;
import com.CristoMorado_api.CristoMorado_api.entity.SeccionCurso;
import com.CristoMorado_api.CristoMorado_api.exception.AccesoDenegadoException;
import com.CristoMorado_api.CristoMorado_api.entity.HorarioBloque;
import com.CristoMorado_api.CristoMorado_api.repository.EstudianteRepository;
import com.CristoMorado_api.CristoMorado_api.repository.HorarioBloqueRepository;
import com.CristoMorado_api.CristoMorado_api.repository.SeccionCursoRepository;
import com.CristoMorado_api.CristoMorado_api.repository.SeccionRepository;
import com.CristoMorado_api.CristoMorado_api.util.CategoriaCurso;
import com.CristoMorado_api.CristoMorado_api.util.Turnos;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

/**
 * Alumno > Horario y Cursos: lo que el alumno ve de su propia sección.
 * Solo muestra cursos activos de la sección.
 * Docente > Dashboard: su horario semanal con todas las secciones que dicta.
 */
@Service
public class MiHorarioService {

    private final EstudianteRepository estudianteRepository;
    private final SeccionRepository seccionRepository;
    private final SeccionCursoRepository seccionCursoRepository;
    private final HorarioService horarioService;
    private final HorarioBloqueRepository bloqueRepository;
    private final AccesoService accesoService;

    public MiHorarioService(EstudianteRepository estudianteRepository, SeccionRepository seccionRepository,
                            SeccionCursoRepository seccionCursoRepository, HorarioService horarioService,
                            HorarioBloqueRepository bloqueRepository, AccesoService accesoService) {
        this.bloqueRepository = bloqueRepository;
        this.accesoService = accesoService;
        this.estudianteRepository = estudianteRepository;
        this.seccionRepository = seccionRepository;
        this.seccionCursoRepository = seccionCursoRepository;
        this.horarioService = horarioService;
    }

    /** Horario semanal del docente: los bloques de los cursos activos que dicta. */
    @Transactional(readOnly = true)
    public MiHorarioDto.Docente delDocente(Long usuarioId, String nombre) {
        if (usuarioId == null) throw new AccesoDenegadoException("Inicia sesión para ver tu horario.");
        List<SeccionCurso> cursos = seccionCursoRepository.findByDocenteUsuarioId(usuarioId).stream()
                .filter(sc -> "Activo".equals(sc.getEstado()) && "Activa".equals(sc.getSeccion().getEstado()))
                .toList();
        List<MiHorarioDto.BloqueDocente> bloques = new ArrayList<>();
        Set<Long> secciones = new HashSet<>();
        LocalTime min = null, max = null;
        for (SeccionCurso sc : cursos) {
            secciones.add(sc.getSeccion().getId());
            for (HorarioBloque b : bloqueRepository.findBySeccionCursoId(sc.getId())) {
                bloques.add(new MiHorarioDto.BloqueDocente(sc.getId(), sc.getCurso().getNombre(), sc.getCurso().getColor(),
                        sc.getCurso().getCategoria(), sc.getSeccion().getGrado(), sc.getSeccion().getLetra(), b.getDia(),
                        b.getHoraInicio().toString(), b.getHoraFin().toString()));
                if (min == null || b.getHoraInicio().isBefore(min)) min = b.getHoraInicio();
                if (max == null || b.getHoraFin().isAfter(max)) max = b.getHoraFin();
            }
        }
        bloques.sort(Comparator.comparing(MiHorarioDto.BloqueDocente::dia).thenComparing(MiHorarioDto.BloqueDocente::horaInicio));
        int hoy = LocalDate.now().getDayOfWeek().getValue();
        long clasesHoy = bloques.stream().filter(b -> b.dia() == hoy).count();
        return new MiHorarioDto.Docente(nombre, cursos.size(), secciones.size(), clasesHoy,
                min == null ? "07:30" : min.toString(), max == null ? "13:15" : max.toString(), bloques);
    }

    @Transactional(readOnly = true)
    public MiHorarioDto.Respuesta delAlumno(Long usuarioId) {
        if (usuarioId == null) throw new AccesoDenegadoException("Inicia sesión para ver tu horario.");
        Estudiante e = estudianteRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new AccesoDenegadoException("Esta vista es solo para alumnos."));
        return deEstudiante(e);
    }

    /** Padre > Dashboard / Mis Hijos: horario y cursos de uno de sus hijos (solo lectura). */
    @Transactional(readOnly = true)
    public MiHorarioDto.Respuesta delHijo(Long usuarioId, Long estudianteId) {
        if (usuarioId == null) throw new AccesoDenegadoException("Inicia sesión para ver el horario.");
        accesoService.padreLogeado(usuarioId);
        return deEstudiante(accesoService.estudianteVisible(usuarioId, estudianteId));
    }

    private MiHorarioDto.Respuesta deEstudiante(Estudiante e) {
        if (e.getGrado() == null || e.getSeccion() == null) return vacio(e, null);

        Seccion s = seccionRepository.findByGradoAndLetra(e.getGrado(), e.getSeccion()).orElse(null);
        Turnos.Turno t = s == null ? null : Turnos.de(s.getGrado(), s.getLetra(), s.getTurno());
        if (s == null || t == null || !"Activa".equals(s.getEstado())) return vacio(e, t);

        HorarioDto.Respuesta h = horarioService.obtener(s.getId());
        List<HorarioDto.Bloque> bloques = h.bloques().stream().filter(b -> !"Inactivo".equals(b.estadoCurso())).toList();

        List<MiHorarioDto.Curso> cursos = seccionCursoRepository.findBySeccionId(s.getId()).stream()
                .filter(sc -> "Activo".equals(sc.getEstado()))
                .sorted(Comparator.comparingInt((SeccionCurso sc) -> CategoriaCurso.orden(sc.getCurso().getCategoria()))
                        .thenComparing(sc -> sc.getCurso().getNombre(), String.CASE_INSENSITIVE_ORDER))
                .map(sc -> {
                    List<HorarioDto.Bloque> suyos = bloques.stream().filter(b -> b.seccionCursoId().equals(sc.getId())).toList();
                    int minutos = suyos.stream().mapToInt(b -> (int) Duration.between(
                            LocalTime.parse(b.horaInicio()), LocalTime.parse(b.horaFin())).toMinutes()).sum();
                    CategoriaCurso.Info cat = CategoriaCurso.de(sc.getCurso().getCategoria());
                    return new MiHorarioDto.Curso(sc.getId(), sc.getCurso().getNombre(), sc.getCurso().getCategoria(),
                            cat == null ? "Otros" : cat.nombre(), sc.getCurso().getColor(), sc.getDescripcion(),
                            sc.getDocente() == null ? null : sc.getDocente().getNombreCompleto(), minutos, suyos);
                })
                .toList();

        return new MiHorarioDto.Respuesta(true, s.getGrado(), s.getLetra(), t.nombre(), h.inicio(), h.fin(),
                h.recesoInicio(), h.recesoFin(), h.minutosBloque(),
                s.getTutor() == null ? null : s.getTutor().getNombreCompleto(), bloques, cursos);
    }

    private static MiHorarioDto.Respuesta vacio(Estudiante e, Turnos.Turno t) {
        return new MiHorarioDto.Respuesta(false, e.getGrado(), e.getSeccion(), t == null ? null : t.nombre(),
                null, null, null, null, Turnos.MINUTOS_BLOQUE, null, List.of(), List.of());
    }
}
