package com.CristoMorado_api.CristoMorado_api.services;

import com.CristoMorado_api.CristoMorado_api.entity.*;
import com.CristoMorado_api.CristoMorado_api.exception.AccesoDenegadoException;
import com.CristoMorado_api.CristoMorado_api.exception.RecursoNoEncontradoException;
import com.CristoMorado_api.CristoMorado_api.repository.*;
import com.CristoMorado_api.CristoMorado_api.util.RolUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Reglas de "quién puede ver qué" que comparten el aula virtual, la asistencia,
 * las notas y las citas. El usuario logeado llega en la cabecera X-Usuario-Id.
 *  - Docente: sus cursos (seccion_cursos donde es el docente) y las secciones de las que es tutor.
 *  - Alumno: los cursos de su sección (mismo grado y letra).
 *  - Padre/apoderado: lo mismo que sus hijos (Vínculo Padre-Hijo).
 *  - Administración / Directiva: todo (solo lectura en el aula).
 */
@Service
public class AccesoService {

    private final UsuarioRepository usuarioRepository;
    private final DocenteRepository docenteRepository;
    private final EstudianteRepository estudianteRepository;
    private final PadreFamiliaRepository padreRepository;
    private final SeccionRepository seccionRepository;
    private final SeccionCursoRepository seccionCursoRepository;

    public AccesoService(UsuarioRepository usuarioRepository, DocenteRepository docenteRepository,
                         EstudianteRepository estudianteRepository, PadreFamiliaRepository padreRepository,
                         SeccionRepository seccionRepository, SeccionCursoRepository seccionCursoRepository) {
        this.usuarioRepository = usuarioRepository;
        this.docenteRepository = docenteRepository;
        this.estudianteRepository = estudianteRepository;
        this.padreRepository = padreRepository;
        this.seccionRepository = seccionRepository;
        this.seccionCursoRepository = seccionCursoRepository;
    }

    // ------------------------------------------------------------------
    // Usuario logeado
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Usuario usuario(Long usuarioId) {
        if (usuarioId == null) throw new AccesoDenegadoException("Inicia sesión para continuar.");
        Usuario u = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new AccesoDenegadoException("Usuario no válido."));
        if (Boolean.FALSE.equals(u.getEstado())) throw new AccesoDenegadoException("Tu cuenta está inactiva.");
        return u;
    }

    public static boolean esDocente(Usuario u) {
        return RolUtil.categoria(u.getRol()) == RolUtil.Categoria.DOCENTE;
    }

    public static boolean esAlumno(Usuario u) {
        return RolUtil.categoria(u.getRol()) == RolUtil.Categoria.ALUMNO;
    }

    public static boolean esPadre(Usuario u) {
        return RolUtil.categoria(u.getRol()) == RolUtil.Categoria.PADRE;
    }

    public static boolean esAdministrativo(Usuario u) {
        return RolUtil.esAdministrativo(u.getRol());
    }

    /** Docente o Administración/Directiva: los que atienden reuniones. */
    public static boolean esResponsable(Usuario u) {
        return esDocente(u) || esAdministrativo(u);
    }

    // ------------------------------------------------------------------
    // Docente
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Usuario exigirDocente(Long usuarioId) {
        Usuario u = usuario(usuarioId);
        if (!esDocente(u)) throw new AccesoDenegadoException("Esta opción es solo para docentes.");
        return u;
    }

    /** Curso que dicta el docente logeado (si no es suyo, 403). */
    @Transactional(readOnly = true)
    public SeccionCurso cursoDelDocente(Long usuarioId, Long seccionCursoId) {
        exigirDocente(usuarioId);
        SeccionCurso sc = seccionCurso(seccionCursoId);
        if (!esDocenteDe(usuarioId, sc)) throw new AccesoDenegadoException("Este curso no está a tu cargo.");
        return sc;
    }

    public static boolean esDocenteDe(Long usuarioId, SeccionCurso sc) {
        return sc.getDocente() != null && usuarioId != null && usuarioId.equals(sc.getDocente().getUsuarioId());
    }

    /** Cursos (activos o no) que dicta el docente. */
    @Transactional(readOnly = true)
    public List<SeccionCurso> cursosDelDocente(Long usuarioId) {
        return seccionCursoRepository.findByDocenteUsuarioId(usuarioId);
    }

    /** Secciones donde el docente enseña algún curso o es tutor. */
    @Transactional(readOnly = true)
    public Set<Long> seccionesDelDocente(Long usuarioId) {
        Set<Long> ids = new LinkedHashSet<>();
        for (SeccionCurso sc : cursosDelDocente(usuarioId)) {
            if ("Activo".equals(sc.getEstado())) ids.add(sc.getSeccion().getId());
        }
        seccionRepository.findByTutorUsuarioId(usuarioId).forEach(s -> ids.add(s.getId()));
        return ids;
    }

    @Transactional(readOnly = true)
    public Optional<Docente> docenteDe(Long usuarioId) {
        return docenteRepository.findByUsuarioId(usuarioId);
    }

    // ------------------------------------------------------------------
    // Alumno y padre
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Estudiante alumnoLogeado(Long usuarioId) {
        usuario(usuarioId);
        return estudianteRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new AccesoDenegadoException("Esta opción es solo para alumnos."));
    }

    @Transactional(readOnly = true)
    public PadreFamilia padreLogeado(Long usuarioId) {
        usuario(usuarioId);
        return padreRepository.findByUsuarioId(usuarioId)
                .orElseThrow(() -> new AccesoDenegadoException("Esta opción es solo para padres de familia."));
    }

    /** Hijos vinculados al padre (Vínculo Padre-Hijo), por nombre. */
    @Transactional(readOnly = true)
    public List<Estudiante> hijos(PadreFamilia p) {
        return p.getVinculosFamiliares().stream()
                .map(EstudianteFamiliar::getEstudiante)
                .sorted(Comparator.comparing(Estudiante::getNombreCompleto, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    /** Padres (con cuenta) de un alumno. */
    @Transactional(readOnly = true)
    public List<Usuario> padresDe(Estudiante e) {
        List<Usuario> lista = new ArrayList<>();
        for (EstudianteFamiliar v : e.getVinculosFamiliares()) {
            Usuario u = v.getPadreFamilia().getUsuario();
            if (u != null && Boolean.TRUE.equals(u.getEstado())) lista.add(u);
        }
        return lista;
    }

    /**
     * Alumno que el usuario puede consultar: él mismo (alumno), su hijo (padre),
     * un alumno de sus secciones (docente) o cualquiera (administración).
     */
    @Transactional(readOnly = true)
    public Estudiante estudianteVisible(Long usuarioId, Long estudianteId) {
        Usuario u = usuario(usuarioId);
        Estudiante e = estudianteRepository.findById(estudianteId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Alumno no encontrado."));
        if (esAdministrativo(u)) return e;
        if (esAlumno(u) && e.getUsuario() != null && e.getUsuario().getId().equals(u.getId())) return e;
        if (esPadre(u)) {
            PadreFamilia p = padreRepository.findByUsuarioId(u.getId()).orElse(null);
            if (p != null && hijos(p).stream().anyMatch(h -> h.getId().equals(e.getId()))) return e;
        }
        if (esDocente(u)) {
            Optional<Seccion> s = seccionDe(e);
            if (s.isPresent() && seccionesDelDocente(u.getId()).contains(s.get().getId())) return e;
        }
        throw new AccesoDenegadoException("No tienes acceso a la información de este alumno.");
    }

    // ------------------------------------------------------------------
    // Secciones y cursos
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public SeccionCurso seccionCurso(Long id) {
        if (id == null) throw new RecursoNoEncontradoException("Curso no encontrado.");
        return seccionCursoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Curso no encontrado."));
    }

    @Transactional(readOnly = true)
    public Optional<Seccion> seccionDe(Estudiante e) {
        if (e.getGrado() == null || e.getSeccion() == null || e.getGrado().isBlank() || e.getSeccion().isBlank()) {
            return Optional.empty();
        }
        return seccionRepository.findByGradoAndLetra(e.getGrado(), e.getSeccion());
    }

    public static boolean estaEn(Estudiante e, Seccion s) {
        return Objects.equals(e.getGrado(), s.getGrado()) && Objects.equals(e.getSeccion(), s.getLetra());
    }

    /** Alumnos matriculados (no inactivos) en la sección, por nombre. */
    @Transactional(readOnly = true)
    public List<Estudiante> alumnosDe(Seccion s) {
        return estudianteRepository.findByGradoAndSeccionOrderByNombreCompletoAsc(s.getGrado(), s.getLetra()).stream()
                .filter(e -> !"Inactivo".equalsIgnoreCase(e.getEstado()))
                .toList();
    }

    /** Cuentas activas de los alumnos de la sección (para avisos). */
    @Transactional(readOnly = true)
    public List<Usuario> cuentasAlumnos(Seccion s) {
        List<Usuario> lista = new ArrayList<>();
        for (Estudiante e : alumnosDe(s)) {
            if (e.getUsuario() != null && Boolean.TRUE.equals(e.getUsuario().getEstado())) lista.add(e.getUsuario());
        }
        return lista;
    }

    /** ¿El usuario puede ver el contenido de este curso? */
    @Transactional(readOnly = true)
    public boolean puedeVer(Usuario u, SeccionCurso sc) {
        if (esAdministrativo(u)) return true;
        if (esDocente(u)) return esDocenteDe(u.getId(), sc);
        if (esAlumno(u)) {
            return estudianteRepository.findByUsuarioId(u.getId()).map(e -> estaEn(e, sc.getSeccion())).orElse(false)
                    && "Activo".equals(sc.getEstado());
        }
        if (esPadre(u)) {
            PadreFamilia p = padreRepository.findByUsuarioId(u.getId()).orElse(null);
            return p != null && hijos(p).stream().anyMatch(h -> estaEn(h, sc.getSeccion()));
        }
        return false;
    }

    @Transactional(readOnly = true)
    public SeccionCurso cursoVisible(Long usuarioId, Long seccionCursoId) {
        Usuario u = usuario(usuarioId);
        SeccionCurso sc = seccionCurso(seccionCursoId);
        if (!puedeVer(u, sc)) throw new AccesoDenegadoException("No tienes acceso a este curso.");
        return sc;
    }

    /** "3° Primaria A" */
    public static String salon(Seccion s) {
        return s.getGrado() + " " + s.getLetra();
    }

    public static String salon(Estudiante e) {
        if (e.getGrado() == null || e.getGrado().isBlank()) return "Sin matrícula";
        return e.getGrado() + (e.getSeccion() == null ? "" : " " + e.getSeccion());
    }
}
