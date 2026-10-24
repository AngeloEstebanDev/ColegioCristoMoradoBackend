package com.CristoMorado_api.CristoMorado_api.services;

import com.CristoMorado_api.CristoMorado_api.dto.CursoResponse;
import com.CristoMorado_api.CristoMorado_api.dto.SeccionRequest;
import com.CristoMorado_api.CristoMorado_api.dto.SeccionResponse;
import com.CristoMorado_api.CristoMorado_api.entity.*;
import com.CristoMorado_api.CristoMorado_api.exception.DatosInvalidosException;
import com.CristoMorado_api.CristoMorado_api.exception.RecursoDuplicadoException;
import com.CristoMorado_api.CristoMorado_api.exception.RecursoNoEncontradoException;
import com.CristoMorado_api.CristoMorado_api.repository.*;
import com.CristoMorado_api.CristoMorado_api.util.Grados;
import com.CristoMorado_api.CristoMorado_api.util.Turnos;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

/**
 * Académico > Grados y Secciones.
 * Los alumnos de una sección son los estudiantes con el mismo grado y letra:
 * matricular = poner ese grado/sección al alumno; retirar = dejarlo sin matrícula.
 */
@Service
public class SeccionService {

    public static final int CAPACIDAD_MAX = 60;

    private final SeccionRepository seccionRepository;
    private final SeccionCursoRepository seccionCursoRepository;
    private final CursoRepository cursoRepository;
    private final EstudianteRepository estudianteRepository;
    private final DocenteRepository docenteRepository;

    public SeccionService(SeccionRepository seccionRepository, SeccionCursoRepository seccionCursoRepository,
                          CursoRepository cursoRepository, EstudianteRepository estudianteRepository,
                          DocenteRepository docenteRepository) {
        this.seccionRepository = seccionRepository;
        this.seccionCursoRepository = seccionCursoRepository;
        this.cursoRepository = cursoRepository;
        this.estudianteRepository = estudianteRepository;
        this.docenteRepository = docenteRepository;
    }

    // ------------------------------------------------------------------
    // Consultas
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<SeccionResponse> listar(String grado) {
        return seccionRepository.findAll().stream()
                .filter(s -> grado == null || grado.isBlank() || s.getGrado().equals(grado.trim()))
                .sorted(Comparator.comparingInt((Seccion s) -> Grados.orden(s.getGrado()))
                        .thenComparing(Seccion::getLetra))
                .map(this::resumen)
                .toList();
    }

    @Transactional(readOnly = true)
    public SeccionResponse obtener(Long id) {
        return detalle(buscar(id));
    }

    @Transactional(readOnly = true)
    public List<CursoResponse> catalogoCursos() {
        return cursoRepository.findByEstadoOrderByNombreAsc("Activo").stream().map(CursoResponse::fromEntity).toList();
    }

    // ------------------------------------------------------------------
    // Sección
    // ------------------------------------------------------------------

    @Transactional
    public SeccionResponse crear(SeccionRequest req) {
        if (req == null || !Grados.valido(req.grado())) throw new DatosInvalidosException("Selecciona un grado válido.");
        String grado = req.grado().trim();
        String letra = req.letra() == null ? "" : req.letra().trim().toUpperCase();
        if (!letra.matches("[A-Z]")) throw new DatosInvalidosException("La sección debe ser una letra de la A a la Z.");
        // Secciones predefinidas: Primaria A (1°-6°) mañana, Primaria B (2°-5°) tarde, Secundaria A mañana.
        // Otras secciones (C, D... o B en 1°, 6° y Secundaria): se elige el turno Mañana o Tarde.
        Turnos.Turno turno = Turnos.de(grado, letra);
        if (turno == null) {
            turno = Turnos.deTurno(grado, req.turno());
            if (turno == null) throw new DatosInvalidosException("Selecciona el turno de la sección " + letra + ": Mañana o Tarde.");
        }
        if (seccionRepository.existsByGradoAndLetraIgnoreCase(grado, letra)) {
            throw new RecursoDuplicadoException("Ya existe la sección " + letra + " en " + grado + ".");
        }
        Seccion s = Seccion.builder().grado(grado).letra(letra)
                .turno(turno.nombre()).capacidad(capacidad(req.capacidad(), 0)).build();
        if (req.tutorId() != null) s.setTutor(tutorLibre(req.tutorId(), null));
        return detalle(seccionRepository.save(s));
    }

    /** Lápiz del detalle: solo la capacidad (no menor que los alumnos matriculados). El turno lo fija la letra. */
    @Transactional
    public SeccionResponse actualizar(Long id, SeccionRequest req) {
        Seccion s = buscar(id);
        if (req == null) throw new DatosInvalidosException("Faltan los datos de la sección.");
        s.setCapacidad(capacidad(req.capacidad(), contarAlumnos(s)));
        return detalle(seccionRepository.save(s));
    }

    @Transactional
    public SeccionResponse cambiarEstado(Long id, boolean activa) {
        Seccion s = buscar(id);
        s.setEstado(activa ? "Activa" : "Inactiva");
        return resumen(seccionRepository.save(s));
    }

    /** docenteId null = quitar tutor. */
    @Transactional
    public SeccionResponse cambiarTutor(Long id, Long docenteId) {
        Seccion s = buscar(id);
        s.setTutor(docenteId == null ? null : tutorLibre(docenteId, s.getId()));
        return detalle(seccionRepository.save(s));
    }

    // ------------------------------------------------------------------
    // Alumnos
    // ------------------------------------------------------------------

    @Transactional
    public SeccionResponse matricular(Long id, Long estudianteId) {
        Seccion s = buscar(id);
        if (!"Activa".equals(s.getEstado())) throw new DatosInvalidosException("La sección está desactivada. Actívala para matricular.");
        if (estudianteId == null) throw new DatosInvalidosException("Selecciona al alumno.");
        Estudiante e = estudianteRepository.findById(estudianteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Alumno no encontrado."));
        if (s.getGrado().equals(e.getGrado()) && s.getLetra().equals(e.getSeccion())) {
            throw new RecursoDuplicadoException(e.getNombreCompleto() + " ya está matriculado en esta sección.");
        }
        if (contarAlumnos(s) >= s.getCapacidad()) {
            throw new DatosInvalidosException("La sección está llena (" + s.getCapacidad() + " alumnos).");
        }
        // Si estaba en otra sección, se cambia a esta.
        e.setGrado(s.getGrado());
        e.setSeccion(s.getLetra());
        estudianteRepository.save(e);
        return detalle(s);
    }

    @Transactional
    public SeccionResponse retirar(Long id, Long estudianteId) {
        Seccion s = buscar(id);
        Estudiante e = estudianteRepository.findById(estudianteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Alumno no encontrado."));
        if (!s.getGrado().equals(e.getGrado()) || !s.getLetra().equals(e.getSeccion())) {
            throw new DatosInvalidosException(e.getNombreCompleto() + " no está matriculado en esta sección.");
        }
        e.setGrado(null);
        e.setSeccion(null);
        estudianteRepository.save(e);
        return detalle(s);
    }

    // ------------------------------------------------------------------
    // Cursos
    // ------------------------------------------------------------------

    @Transactional
    public SeccionResponse agregarCurso(Long id, Long cursoId, Long docenteId, String descripcion) {
        Seccion s = buscar(id);
        if (cursoId == null) throw new DatosInvalidosException("Selecciona el curso.");
        Curso c = cursoRepository.findById(cursoId)
                .filter(x -> "Activo".equals(x.getEstado()))
                .orElseThrow(() -> new RecursoNoEncontradoException("Curso no encontrado en el catálogo."));
        if (seccionCursoRepository.existsBySeccionIdAndCursoId(s.getId(), c.getId())) {
            throw new RecursoDuplicadoException(c.getNombre() + " ya está asignado a esta sección.");
        }
        String desc = descripcion == null || descripcion.isBlank() ? null : descripcion.trim();
        if (desc != null && desc.length() > 300) throw new DatosInvalidosException("La descripción admite hasta 300 caracteres.");
        seccionCursoRepository.save(SeccionCurso.builder().seccion(s).curso(c).descripcion(desc)
                .docente(docenteId == null ? null : docenteActivo(docenteId)).build());
        return detalle(s);
    }

    /** docenteId null = quitar docente. */
    @Transactional
    public SeccionResponse asignarDocente(Long id, Long seccionCursoId, Long docenteId) {
        SeccionCurso sc = cursoDeSeccion(id, seccionCursoId);
        sc.setDocente(docenteId == null ? null : docenteActivo(docenteId));
        seccionCursoRepository.save(sc);
        return detalle(sc.getSeccion());
    }

    /** Editar la descripción (comentario) de un curso ya agregado a la sección. Vacío = sin descripción. */
    @Transactional
    public SeccionResponse editarDescripcion(Long id, Long seccionCursoId, String descripcion) {
        SeccionCurso sc = cursoDeSeccion(id, seccionCursoId);
        String desc = descripcion == null || descripcion.isBlank() ? null : descripcion.trim();
        if (desc != null && desc.length() > 300) throw new DatosInvalidosException("La descripción admite hasta 300 caracteres.");
        sc.setDescripcion(desc);
        seccionCursoRepository.save(sc);
        return detalle(sc.getSeccion());
    }

    @Transactional
    public SeccionResponse cambiarEstadoCurso(Long id, Long seccionCursoId, boolean activo) {
        SeccionCurso sc = cursoDeSeccion(id, seccionCursoId);
        sc.setEstado(activo ? "Activo" : "Inactivo");
        seccionCursoRepository.save(sc);
        return detalle(sc.getSeccion());
    }

    // ------------------------------------------------------------------
    // Ayudas
    // ------------------------------------------------------------------

    private SeccionResponse resumen(Seccion s) {
        long cursos = seccionCursoRepository.findBySeccionId(s.getId()).size();
        return SeccionResponse.of(s, contarAlumnos(s), cursos, List.of(), List.of());
    }

    private SeccionResponse detalle(Seccion s) {
        List<SeccionResponse.Alumno> alumnos = estudianteRepository
                .findByGradoAndSeccionOrderByNombreCompletoAsc(s.getGrado(), s.getLetra()).stream()
                .map(e -> new SeccionResponse.Alumno(e.getId(), e.getNombreCompleto(),
                        e.getUsuario() == null ? null : e.getUsuario().getUsuario(),
                        e.getTipoDocumento(), e.getDni(), e.getEstado(),
                        e.getUsuario() == null ? null : e.getUsuario().getId()))
                .toList();
        List<SeccionResponse.CursoAsignado> cursos = seccionCursoRepository.findBySeccionId(s.getId()).stream()
                .sorted(Comparator.comparing((SeccionCurso sc) -> sc.getCurso().getNombre(), String.CASE_INSENSITIVE_ORDER))
                .map(sc -> new SeccionResponse.CursoAsignado(sc.getId(), sc.getCurso().getId(), sc.getCurso().getNombre(),
                        sc.getCurso().getColor(), sc.getDescripcion(),
                        sc.getDocente() == null ? null : sc.getDocente().getId(),
                        sc.getDocente() == null ? null : sc.getDocente().getNombreCompleto(),
                        sc.getEstado(),
                        sc.getDocente() == null ? null : sc.getDocente().getUsuarioId()))
                .toList();
        return SeccionResponse.of(s, alumnos.size(), cursos.size(), alumnos, cursos);
    }

    private long contarAlumnos(Seccion s) {
        return estudianteRepository.countByGradoAndSeccion(s.getGrado(), s.getLetra());
    }

    private SeccionCurso cursoDeSeccion(Long seccionId, Long seccionCursoId) {
        SeccionCurso sc = seccionCursoRepository.findById(seccionCursoId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Curso de la sección no encontrado."));
        if (!sc.getSeccion().getId().equals(seccionId)) {
            throw new RecursoNoEncontradoException("Curso de la sección no encontrado.");
        }
        return sc;
    }

    /** El docente debe estar activo y no ser tutor de otra sección. */
    private Docente tutorLibre(Long docenteId, Long seccionId) {
        Docente d = docenteActivo(docenteId);
        seccionRepository.findFirstByTutorId(d.getId())
                .filter(otra -> !otra.getId().equals(seccionId))
                .ifPresent(otra -> {
                    throw new DatosInvalidosException(d.getNombreCompleto() + " ya es tutor(a) de " + otra.getGrado()
                            + " - Sección " + otra.getLetra() + ". Un docente solo puede ser tutor de una sección.");
                });
        return d;
    }

    private Docente docenteActivo(Long docenteId) {
        Docente d = docenteRepository.findById(docenteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Docente no encontrado."));
        if ("Inactivo".equalsIgnoreCase(d.getEstado())) {
            throw new DatosInvalidosException(d.getNombreCompleto() + " está inactivo.");
        }
        return d;
    }

    private static int capacidad(Integer capacidad, long minimo) {
        int c = capacidad == null ? 35 : capacidad;
        if (c < 1 || c > CAPACIDAD_MAX) throw new DatosInvalidosException("La capacidad debe estar entre 1 y " + CAPACIDAD_MAX + ".");
        if (c < minimo) throw new DatosInvalidosException("La capacidad no puede ser menor a los " + minimo + " alumnos matriculados.");
        return c;
    }

    private Seccion buscar(Long id) {
        return seccionRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Sección no encontrada."));
    }
}
