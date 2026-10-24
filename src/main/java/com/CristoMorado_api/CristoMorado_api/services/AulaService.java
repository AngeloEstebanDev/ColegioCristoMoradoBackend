package com.CristoMorado_api.CristoMorado_api.services;

import com.CristoMorado_api.CristoMorado_api.dto.ArchivoDto;
import com.CristoMorado_api.CristoMorado_api.dto.AulaDto;
import com.CristoMorado_api.CristoMorado_api.entity.*;
import com.CristoMorado_api.CristoMorado_api.exception.AccesoDenegadoException;
import com.CristoMorado_api.CristoMorado_api.exception.DatosInvalidosException;
import com.CristoMorado_api.CristoMorado_api.exception.RecursoNoEncontradoException;
import com.CristoMorado_api.CristoMorado_api.repository.*;
import com.CristoMorado_api.CristoMorado_api.util.Calificacion;
import com.CristoMorado_api.CristoMorado_api.util.CategoriaCurso;
import com.CristoMorado_api.CristoMorado_api.util.Grados;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Aula virtual:
 *  HU-13 Materiales compartidos (el docente publica archivos/enlaces; alumnos y padres los ven y descargan).
 *  HU-14 Actividades académicas (tareas, cuestionarios de opción múltiple y actividades).
 *  HU-15 Entregas de los alumnos (el docente las califica; la nota pasa al registro de notas).
 */
@Service
public class AulaService {

    private static final Set<String> TIPOS = Set.of(Actividad.TAREA, Actividad.CUESTIONARIO, Actividad.ACTIVIDAD);
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final MaterialRepository materialRepository;
    private final ActividadRepository actividadRepository;
    private final EntregaRepository entregaRepository;
    private final SeccionCursoRepository seccionCursoRepository;
    private final SeccionRepository seccionRepository;
    private final EstudianteRepository estudianteRepository;
    private final UsuarioRepository usuarioRepository;
    private final EvaluacionRepository evaluacionRepository;
    private final AccesoService accesoService;
    private final ArchivoService archivoService;
    private final NotaService notaService;
    private final MensajeService mensajeService;
    private final CarpetaRepository carpetaRepository;

    public AulaService(MaterialRepository materialRepository, ActividadRepository actividadRepository,
                       EntregaRepository entregaRepository, SeccionCursoRepository seccionCursoRepository,
                       SeccionRepository seccionRepository, EstudianteRepository estudianteRepository,
                       UsuarioRepository usuarioRepository, EvaluacionRepository evaluacionRepository,
                       AccesoService accesoService,
                       ArchivoService archivoService, NotaService notaService, MensajeService mensajeService,
                       CarpetaRepository carpetaRepository) {
        this.materialRepository = materialRepository;
        this.actividadRepository = actividadRepository;
        this.entregaRepository = entregaRepository;
        this.seccionCursoRepository = seccionCursoRepository;
        this.seccionRepository = seccionRepository;
        this.estudianteRepository = estudianteRepository;
        this.usuarioRepository = usuarioRepository;
        this.evaluacionRepository = evaluacionRepository;
        this.accesoService = accesoService;
        this.archivoService = archivoService;
        this.notaService = notaService;
        this.mensajeService = mensajeService;
        this.carpetaRepository = carpetaRepository;
    }

    // ==================================================================
    // Cursos
    // ==================================================================

    /** Docente: los cursos que dicta (solo asignaciones y secciones activas). */
    @Transactional(readOnly = true)
    public List<AulaDto.CursoAula> cursosDocente(Long usuarioId) {
        accesoService.exigirDocente(usuarioId);
        return accesoService.cursosDelDocente(usuarioId).stream()
                .filter(sc -> "Activo".equals(sc.getEstado()) && "Activa".equals(sc.getSeccion().getEstado()))
                .sorted(ordenCursos())
                .map(sc -> cursoAula(sc, null))
                .toList();
    }

    @Transactional(readOnly = true)
    public AulaDto.ResumenDocente resumenDocente(Long usuarioId) {
        Usuario u = accesoService.exigirDocente(usuarioId);
        List<AulaDto.CursoAula> cursos = cursosDocente(usuarioId);
        Set<Long> secciones = cursos.stream().map(AulaDto.CursoAula::seccionId).collect(Collectors.toSet());
        long alumnos = secciones.stream().mapToLong(id -> seccionRepository.findById(id)
                .map(s -> (long) accesoService.alumnosDe(s).size()).orElse(0L)).sum();
        long porCalificar = cursos.stream().mapToLong(AulaDto.CursoAula::porCalificar).sum();
        String tutor = seccionRepository.findByTutorUsuarioId(usuarioId).stream()
                .map(AccesoService::salon).findFirst().orElse(null);
        LocalDateTime ahora = LocalDateTime.now();
        List<Long> ids = cursos.stream().map(AulaDto.CursoAula::seccionCursoId).toList();
        List<AulaDto.ActividadResponse> proximas = ids.isEmpty() ? List.of()
                : actividadRepository.findBySeccionCursoIdInOrderByFechaLimiteAsc(ids).stream()
                    .filter(a -> a.getFechaLimite().isAfter(ahora.minusDays(1)))
                    .limit(6)
                    .map(a -> actividadParaDocente(a, false))
                    .toList();
        return new AulaDto.ResumenDocente(u.getNombre(), cursos.size(), secciones.size(), alumnos, porCalificar,
                tutor, cursos, proximas);
    }

    /** Alumno: los cursos activos de su sección, con sus actividades pendientes. */
    @Transactional(readOnly = true)
    public List<AulaDto.CursoAula> cursosAlumno(Long usuarioId) {
        Estudiante e = accesoService.alumnoLogeado(usuarioId);
        return cursosDeEstudiante(e);
    }

    /** Padre: los cursos de un hijo. */
    @Transactional(readOnly = true)
    public List<AulaDto.CursoAula> cursosHijo(Long usuarioId, Long estudianteId) {
        return cursosDeEstudiante(accesoService.estudianteVisible(usuarioId, estudianteId));
    }

    @Transactional(readOnly = true)
    public AulaDto.CursoAula curso(Long usuarioId, Long seccionCursoId) {
        Usuario u = accesoService.usuario(usuarioId);
        SeccionCurso sc = accesoService.cursoVisible(usuarioId, seccionCursoId);
        Estudiante e = AccesoService.esAlumno(u) ? accesoService.alumnoLogeado(usuarioId) : null;
        return cursoAula(sc, e);
    }

    /** Docente y compañeros del curso (vista "Mis cursos" del alumno). */
    @Transactional(readOnly = true)
    public AulaDto.Participantes participantes(Long usuarioId, Long seccionCursoId) {
        SeccionCurso sc = accesoService.cursoVisible(usuarioId, seccionCursoId);
        Docente d = sc.getDocente();
        AulaDto.Participante docente = null;
        if (d != null) {
            Usuario cuenta = d.getUsuarioId() == null ? null : usuarioRepository.findById(d.getUsuarioId()).orElse(null);
            String detalle = "Docente — " + (d.getEspecialidad() == null || d.getEspecialidad().isBlank()
                    ? sc.getCurso().getNombre() : d.getEspecialidad());
            docente = new AulaDto.Participante(d.getUsuarioId(), d.getNombreCompleto(), detalle,
                    cuenta == null ? null : cuenta.getFoto());
        }
        List<AulaDto.Participante> alumnos = accesoService.alumnosDe(sc.getSeccion()).stream()
                .map(e -> new AulaDto.Participante(e.getUsuario() == null ? null : e.getUsuario().getId(),
                        e.getNombreCompleto(), "Estudiante", e.getUsuario() == null ? null : e.getUsuario().getFoto()))
                .toList();
        return new AulaDto.Participantes(docente, alumnos);
    }

    // ==================================================================
    // Materiales (HU-13)
    // ==================================================================

    @Transactional(readOnly = true)
    public List<AulaDto.MaterialResponse> materiales(Long usuarioId, Long seccionCursoId) {
        SeccionCurso sc = accesoService.cursoVisible(usuarioId, seccionCursoId);
        boolean todo = veTodo(usuarioId, sc);
        Set<Long> ocultas = todo ? Set.of() : carpetasOcultas(sc);
        return materialRepository.findBySeccionCursoIdOrderByFechaPublicacionDesc(sc.getId()).stream()
                .filter(m -> todo || (!Boolean.FALSE.equals(m.getVisible()) && !ocultas.contains(m.getCarpetaId())))
                .map(this::material).toList();
    }

    @Transactional
    public AulaDto.MaterialResponse crearMaterial(Long usuarioId, Long seccionCursoId, AulaDto.MaterialRequest req) {
        SeccionCurso sc = accesoService.cursoDelDocente(usuarioId, seccionCursoId);
        Material m = Material.builder().seccionCurso(sc).autorId(usuarioId).build();
        m.setCarpetaId(carpetaDelCurso(sc, req == null ? null : req.carpetaId()));
        aplicarMaterial(m, req, usuarioId);
        m = materialRepository.save(m);
        avisarSeccion(sc, "Nuevo material en " + sc.getCurso().getNombre(),
                "<p>Tu docente publicó el material <strong>" + MensajeService.escapar(m.getTitulo())
                        + "</strong> en <strong>" + MensajeService.escapar(sc.getCurso().getNombre())
                        + "</strong>.</p><p>Revísalo en <em>Mis Cursos</em>.</p>");
        return material(m);
    }

    @Transactional
    public AulaDto.MaterialResponse actualizarMaterial(Long usuarioId, Long materialId, AulaDto.MaterialRequest req) {
        Material m = materialRepository.findById(materialId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Material no encontrado."));
        accesoService.cursoDelDocente(usuarioId, m.getSeccionCurso().getId());
        if (req != null && req.carpetaId() != null) m.setCarpetaId(carpetaDelCurso(m.getSeccionCurso(), req.carpetaId()));
        aplicarMaterial(m, req, usuarioId);
        m.setFechaActualizacion(LocalDateTime.now());
        return material(materialRepository.save(m));
    }

    @Transactional
    public void eliminarMaterial(Long usuarioId, Long materialId) {
        Material m = materialRepository.findById(materialId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Material no encontrado."));
        accesoService.cursoDelDocente(usuarioId, m.getSeccionCurso().getId());
        Long archivo = m.getArchivoId();
        materialRepository.delete(m);
        materialRepository.flush();
        archivoService.eliminar(archivo);
    }

    /** Ocultar / mostrar un material a los alumnos. */
    @Transactional
    public AulaDto.MaterialResponse visibleMaterial(Long usuarioId, Long materialId, Boolean visible) {
        Material m = materialRepository.findById(materialId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Material no encontrado."));
        accesoService.cursoDelDocente(usuarioId, m.getSeccionCurso().getId());
        m.setVisible(!Boolean.FALSE.equals(visible));
        return material(materialRepository.save(m));
    }

    // ==================================================================
    // Carpetas del contenido (se crean en el bimestre en curso)
    // ==================================================================

    @Transactional(readOnly = true)
    public List<AulaDto.Carpeta> carpetas(Long usuarioId, Long seccionCursoId) {
        SeccionCurso sc = accesoService.cursoVisible(usuarioId, seccionCursoId);
        boolean todo = veTodo(usuarioId, sc);
        return carpetaRepository.findBySeccionCursoIdOrderByFechaCreacionAsc(sc.getId()).stream()
                .filter(c -> todo || !Boolean.FALSE.equals(c.getVisible()))
                .map(AulaService::carpeta).toList();
    }

    @Transactional
    public AulaDto.Carpeta crearCarpeta(Long usuarioId, Long seccionCursoId, AulaDto.CarpetaRequest req) {
        SeccionCurso sc = accesoService.cursoDelDocente(usuarioId, seccionCursoId);
        Periodo p = notaService.periodoActivo();
        if (p == null) {
            throw new DatosInvalidosException("No hay un bimestre en curso. Pide a la administración que active el periodo.");
        }
        String nombre = nombreCarpeta(req == null ? null : req.nombre());
        boolean repetida = carpetaRepository.findBySeccionCursoIdOrderByFechaCreacionAsc(sc.getId()).stream()
                .anyMatch(c -> c.getPeriodo().getId().equals(p.getId()) && c.getNombre().equalsIgnoreCase(nombre));
        if (repetida) throw new DatosInvalidosException("Ya existe la carpeta \"" + nombre + "\" en este bimestre.");
        return carpeta(carpetaRepository.save(Carpeta.builder().seccionCurso(sc).periodo(p).nombre(nombre).build()));
    }

    @Transactional
    public AulaDto.Carpeta renombrarCarpeta(Long usuarioId, Long carpetaId, AulaDto.CarpetaRequest req) {
        Carpeta c = buscarCarpeta(carpetaId);
        accesoService.cursoDelDocente(usuarioId, c.getSeccionCurso().getId());
        c.setNombre(nombreCarpeta(req == null ? null : req.nombre()));
        return carpeta(carpetaRepository.save(c));
    }

    @Transactional
    public AulaDto.Carpeta visibleCarpeta(Long usuarioId, Long carpetaId, Boolean visible) {
        Carpeta c = buscarCarpeta(carpetaId);
        accesoService.cursoDelDocente(usuarioId, c.getSeccionCurso().getId());
        c.setVisible(!Boolean.FALSE.equals(visible));
        return carpeta(carpetaRepository.save(c));
    }

    /** Solo se puede borrar una carpeta vacía. */
    @Transactional
    public void eliminarCarpeta(Long usuarioId, Long carpetaId) {
        Carpeta c = buscarCarpeta(carpetaId);
        accesoService.cursoDelDocente(usuarioId, c.getSeccionCurso().getId());
        Long sc = c.getSeccionCurso().getId();
        boolean conContenido = materialRepository.findBySeccionCursoIdOrderByFechaPublicacionDesc(sc).stream()
                .anyMatch(m -> c.getId().equals(m.getCarpetaId()))
                || actividadRepository.findBySeccionCursoIdOrderByFechaLimiteDesc(sc).stream()
                .anyMatch(a -> c.getId().equals(a.getCarpetaId()));
        if (conContenido) throw new DatosInvalidosException("La carpeta tiene contenido. Elimínalo o muévelo antes de borrarla.");
        carpetaRepository.delete(c);
    }

    // ==================================================================
    // Actividades (HU-14)
    // ==================================================================

    @Transactional(readOnly = true)
    public List<AulaDto.ActividadResponse> actividades(Long usuarioId, Long seccionCursoId) {
        Usuario u = accesoService.usuario(usuarioId);
        SeccionCurso sc = accesoService.cursoVisible(usuarioId, seccionCursoId);
        List<Actividad> lista = actividadRepository.findBySeccionCursoIdOrderByFechaLimiteDesc(sc.getId());
        if (!veTodo(usuarioId, sc)) lista = soloVisibles(sc, lista);
        if (AccesoService.esAlumno(u)) {
            Estudiante e = accesoService.alumnoLogeado(usuarioId);
            return lista.stream().map(a -> actividadParaAlumno(a, e, false)).toList();
        }
        // Padre: ve las actividades como su hijo de esa sección (si entregó, su nota), no las cifras del salón.
        Estudiante hijo = hijoEn(u, sc);
        if (hijo != null) return lista.stream().map(a -> actividadParaAlumno(a, hijo, false)).toList();
        return lista.stream().map(a -> actividadParaDocente(a, false)).toList();
    }

    /** Detalle (con preguntas). El alumno ve su entrega; las respuestas correctas solo después de enviar. */
    @Transactional(readOnly = true)
    public AulaDto.ActividadResponse actividad(Long usuarioId, Long actividadId) {
        Usuario u = accesoService.usuario(usuarioId);
        Actividad a = buscarActividad(actividadId);
        if (!accesoService.puedeVer(u, a.getSeccionCurso())) throw new AccesoDenegadoException("No tienes acceso a esta actividad.");
        if (!veTodo(usuarioId, a.getSeccionCurso()) && soloVisibles(a.getSeccionCurso(), List.of(a)).isEmpty()) {
            throw new RecursoNoEncontradoException("Actividad no encontrada.");
        }
        if (AccesoService.esAlumno(u)) return actividadParaAlumno(a, accesoService.alumnoLogeado(usuarioId), true);
        Estudiante hijo = hijoEn(u, a.getSeccionCurso());
        if (hijo != null) return actividadParaAlumno(a, hijo, true);
        return actividadParaDocente(a, AccesoService.esDocenteDe(usuarioId, a.getSeccionCurso()));
    }

    /** Si u es padre, su hijo matriculado en la sección del curso (o null). */
    private Estudiante hijoEn(Usuario u, SeccionCurso sc) {
        if (!AccesoService.esPadre(u)) return null;
        PadreFamilia p = accesoService.padreLogeado(u.getId());
        return accesoService.hijos(p).stream().filter(h -> AccesoService.estaEn(h, sc.getSeccion())).findFirst().orElse(null);
    }

    @Transactional
    public AulaDto.ActividadResponse crearActividad(Long usuarioId, Long seccionCursoId, AulaDto.ActividadRequest req) {
        SeccionCurso sc = accesoService.cursoDelDocente(usuarioId, seccionCursoId);
        Actividad a = Actividad.builder().seccionCurso(sc).autorId(usuarioId).build();
        a.setCarpetaId(carpetaDelCurso(sc, req == null ? null : req.carpetaId()));
        aplicarActividad(a, req, usuarioId, true);
        a = actividadRepository.save(a);
        notaService.asegurarEvaluacion(a);
        String tipo = etiquetaTipo(a.getTipo()).toLowerCase();
        avisarSeccion(sc, "Nueva " + tipo + " en " + sc.getCurso().getNombre(),
                "<p>Se publicó la " + tipo + " <strong>" + MensajeService.escapar(a.getTitulo()) + "</strong> en <strong>"
                        + MensajeService.escapar(sc.getCurso().getNombre()) + "</strong>.</p><p>Fecha límite: <strong>"
                        + a.getFechaLimite().format(FECHA_HORA) + "</strong>. Entrégala desde <em>Mis Cursos</em>.</p>");
        return actividadParaDocente(a, true);
    }

    @Transactional
    public AulaDto.ActividadResponse actualizarActividad(Long usuarioId, Long actividadId, AulaDto.ActividadRequest req) {
        Actividad a = buscarActividad(actividadId);
        accesoService.cursoDelDocente(usuarioId, a.getSeccionCurso().getId());
        // El docente puede cambiar todo (título, fechas, intentos, archivo, preguntas...) aunque ya haya entregas.
        // Solo el tipo (tarea <-> cuestionario) queda fijo cuando ya hay entregas, porque son incompatibles.
        boolean conEntregas = entregaRepository.countByActividadId(a.getId()) > 0;
        String tipoNuevo = req == null || req.tipo() == null ? "" : req.tipo().trim().toUpperCase();
        if (conEntregas && !tipoNuevo.equals(a.getTipo())) {
            throw new DatosInvalidosException("No se puede cambiar de " + (Actividad.CUESTIONARIO.equals(a.getTipo()) ? "cuestionario" : "tarea")
                    + " a otro tipo porque ya hay entregas. Todo lo demás sí se puede cambiar.");
        }
        if (req != null && req.carpetaId() != null) a.setCarpetaId(carpetaDelCurso(a.getSeccionCurso(), req.carpetaId()));
        aplicarActividad(a, req, usuarioId, true);
        a.setFechaActualizacion(LocalDateTime.now());
        a = actividadRepository.save(a);
        notaService.sincronizarEvaluacion(a);
        if (conEntregas && Actividad.CUESTIONARIO.equals(a.getTipo())) recalificarCuestionario(a, usuarioId);
        return actividadParaDocente(a, true);
    }

    /**
     * Después de editar las preguntas o respuestas correctas de un cuestionario con entregas,
     * vuelve a calificar cada entrega y actualiza su nota en Participación.
     */
    private void recalificarCuestionario(Actividad a, Long usuarioId) {
        List<PreguntaCuestionario> preguntas = a.getPreguntas();
        Usuario docente = docenteUsuario(a.getSeccionCurso());
        for (Entrega en : entregaRepository.findByActividadId(a.getId())) {
            List<Integer> r = new ArrayList<>(en.getRespuestas());
            while (r.size() < preguntas.size()) r.add(-1);      // pregunta nueva: sin responder
            while (r.size() > preguntas.size()) r.remove(r.size() - 1);
            double puntaje = 0;
            for (int i = 0; i < preguntas.size(); i++) {
                if (r.get(i).equals(preguntas.get(i).getRespuestaCorrecta())) puntaje += preguntas.get(i).getPuntos();
            }
            en.getRespuestas().clear();
            en.getRespuestas().addAll(r);
            en.setPuntaje(Calificacion.dosDecimales(puntaje));
            en.setEstado(Entrega.CALIFICADA);
            en.setFechaCalificacion(LocalDateTime.now());
            en.setComentarioDocente("Recalificado automáticamente (el docente editó el cuestionario).");
            entregaRepository.save(en);
            notaService.registrarDesdeActividad(a, en.getEstudiante(), nota20(en.getPuntaje(), a.getPuntajeMaximo()),
                    docente == null ? accesoService.usuario(usuarioId) : docente, null);
        }
    }

    /** Activa o desactiva "aceptar entregas tardías" (el docente lo puede cambiar en cualquier momento). */
    @Transactional
    public AulaDto.ActividadResponse tardiaActividad(Long usuarioId, Long actividadId, Boolean permite) {
        Actividad a = buscarActividad(actividadId);
        accesoService.cursoDelDocente(usuarioId, a.getSeccionCurso().getId());
        a.setPermiteTardia(Boolean.TRUE.equals(permite));
        a.setFechaActualizacion(LocalDateTime.now());
        return actividadParaDocente(actividadRepository.save(a), true);
    }

    /** Ocultar / mostrar una actividad (oculta no cuenta en Participación). */
    @Transactional
    public AulaDto.ActividadResponse visibleActividad(Long usuarioId, Long actividadId, Boolean visible) {
        Actividad a = buscarActividad(actividadId);
        accesoService.cursoDelDocente(usuarioId, a.getSeccionCurso().getId());
        a.setVisible(!Boolean.FALSE.equals(visible));
        return actividadParaDocente(actividadRepository.save(a), true);
    }

    /** Borra la actividad, sus entregas, sus archivos y su columna en el registro de notas. */
    @Transactional
    public void eliminarActividad(Long usuarioId, Long actividadId) {
        Actividad a = buscarActividad(actividadId);
        accesoService.cursoDelDocente(usuarioId, a.getSeccionCurso().getId());
        Usuario u = accesoService.usuario(usuarioId);
        List<Long> archivos = new ArrayList<>();
        for (Entrega e : entregaRepository.findByActividadId(a.getId())) {
            archivos.addAll(archivosDe(e));
            entregaRepository.delete(e);
        }
        if (a.getArchivoId() != null) archivos.add(a.getArchivoId());
        notaService.eliminarEvaluacionDeActividad(a, u);
        actividadRepository.delete(a);
        actividadRepository.flush();
        archivos.stream().distinct().forEach(archivoService::eliminar);
    }

    /** Archivos de una entrega y de todo su historial de envíos. */
    static List<Long> archivosDe(Entrega e) {
        List<Long> res = new ArrayList<>();
        if (e.getArchivoId() != null) res.add(e.getArchivoId());
        for (EntregaEnvio v : e.getEnvios()) {
            if (v.getArchivoId() != null && !res.contains(v.getArchivoId())) res.add(v.getArchivoId());
        }
        return res;
    }

    /** Alumno: todas las actividades de sus cursos (pendientes primero). */
    @Transactional(readOnly = true)
    public List<AulaDto.ActividadResponse> misActividades(Long usuarioId) {
        return actividadesDe(accesoService.alumnoLogeado(usuarioId));
    }

    /** Padre: las actividades de un hijo y si las entregó. */
    @Transactional(readOnly = true)
    public List<AulaDto.ActividadResponse> actividadesHijo(Long usuarioId, Long estudianteId) {
        return actividadesDe(accesoService.estudianteVisible(usuarioId, estudianteId));
    }

    // ==================================================================
    // Entregas (HU-15)
    // ==================================================================

    @Transactional
    public AulaDto.ActividadResponse entregar(Long usuarioId, Long actividadId, AulaDto.EntregaRequest req) {
        Estudiante e = accesoService.alumnoLogeado(usuarioId);
        Actividad a = buscarActividad(actividadId);
        SeccionCurso sc = a.getSeccionCurso();
        if (!AccesoService.estaEn(e, sc.getSeccion()) || !"Activo".equals(sc.getEstado())
                || soloVisibles(sc, List.of(a)).isEmpty()) {
            throw new AccesoDenegadoException("Esta actividad no es de tu sección.");
        }
        if (req == null) throw new DatosInvalidosException("Faltan los datos de la entrega.");
        LocalDateTime ahora = LocalDateTime.now();
        // Después de la fecha límite solo se acepta si el docente activó "entrega tardía".
        boolean tardia = ahora.isAfter(a.getFechaLimite());
        if (tardia && !Boolean.TRUE.equals(a.getPermiteTardia())) {
            throw new DatosInvalidosException("La fecha límite ya pasó (" + a.getFechaLimite().format(FECHA_HORA) + ").");
        }
        int permitidos = intentosPermitidos(a);
        Entrega en = entregaRepository.findByActividadIdAndEstudianteId(a.getId(), e.getId()).orElse(null);
        boolean esCuestionario = Actividad.CUESTIONARIO.equals(a.getTipo());
        if (en != null && !esCuestionario && Entrega.CALIFICADA.equals(en.getEstado())) {
            throw new DatosInvalidosException("Tu entrega ya fue calificada; no se puede modificar.");
        }
        if (en != null && intentos(en) >= permitidos) {
            throw new DatosInvalidosException(permitidos == 1 ? "Ya enviaste esta actividad (solo se permite 1 intento)."
                    : "Ya usaste tus " + permitidos + " intentos.");
        }
        boolean nueva = en == null;
        if (nueva) en = Entrega.builder().actividad(a).estudiante(e).intentos(0).build();
        EntregaEnvio envio = EntregaEnvio.builder().entrega(en).numero(intentos(en) + 1).fecha(ahora).tardia(tardia).build();

        if (esCuestionario) {
            List<PreguntaCuestionario> preguntas = a.getPreguntas();
            List<Integer> limpias = respuestasCuestionario(preguntas, req);
            double puntaje = 0;
            for (int i = 0; i < preguntas.size(); i++) {
                PreguntaCuestionario p = preguntas.get(i);
                if (limpias.get(i).equals(p.getRespuestaCorrecta())) puntaje += p.getPuntos();
            }
            puntaje = Calificacion.dosDecimales(puntaje);
            envio.setPuntaje(puntaje);
            envio.setRespuestas(limpias.stream().map(String::valueOf).collect(Collectors.joining(",")));
            // Con varios intentos se queda el mejor puntaje.
            if (nueva || en.getPuntaje() == null || puntaje >= en.getPuntaje()) {
                en.getRespuestas().clear();
                en.getRespuestas().addAll(limpias);
                en.setPuntaje(puntaje);
                en.setFechaEntrega(ahora);
                en.setTardia(tardia);
            }
            en.setEstado(Entrega.CALIFICADA);
            en.setFechaCalificacion(ahora);
            en.setComentarioDocente("Calificado automáticamente.");
        } else {
            String texto = NotaService.recortar(req.texto(), 20000);
            String enlace = enlace(req.enlace());
            // Solo formatos permitidos por el docente (PDF, Word, Excel, PowerPoint, imágenes) y máx. 10 MB.
            ArchivoAdjunto nuevo = archivoService.guardar(req.archivo(), usuarioId, formatos(a));
            // Cada envío es una versión nueva: lo que no se adjunte de nuevo se conserva del envío anterior.
            boolean tieneArchivo = nuevo != null || en.getArchivoId() != null;
            if (texto == null && enlace == null && !tieneArchivo) {
                throw new DatosInvalidosException("Escribe tu respuesta, pega un enlace o adjunta un archivo.");
            }
            // El archivo anterior NO se borra: queda en el historial ("Entrega 1", "Entrega 2"...).
            if (nuevo != null) ponerArchivo(en, nuevo);
            en.setTexto(texto);
            en.setEnlace(enlace);
            en.setEstado(Entrega.ENTREGADA);
            en.setFechaEntrega(ahora);
            en.setTardia(tardia);
            envio.setTexto(texto);
            envio.setEnlace(enlace);
            envio.setArchivoId(en.getArchivoId());
            envio.setArchivoNombre(en.getArchivoNombre());
            envio.setArchivoTipo(en.getArchivoTipo());
            envio.setArchivoTamanio(en.getArchivoTamanio());
        }
        en.setIntentos(intentos(en) + 1);
        en.getEnvios().add(envio);
        en = entregaRepository.save(en);

        if (Entrega.CALIFICADA.equals(en.getEstado())) {
            Usuario docente = docenteUsuario(sc);
            notaService.registrarDesdeActividad(a, e, nota20(en.getPuntaje(), a.getPuntajeMaximo()),
                    docente == null ? accesoService.usuario(usuarioId) : docente, null);
        }
        return actividadParaAlumno(a, e, true);
    }

    @Transactional(readOnly = true)
    public AulaDto.Entregas entregas(Long usuarioId, Long actividadId) {
        Actividad a = buscarActividad(actividadId);
        Usuario u = accesoService.usuario(usuarioId);
        if (!AccesoService.esDocenteDe(usuarioId, a.getSeccionCurso()) && !AccesoService.esAdministrativo(u)) {
            throw new AccesoDenegadoException("Este curso no está a tu cargo.");
        }
        return construirEntregas(a);
    }

    /** El docente califica (o corrige la calificación de) una entrega. */
    @Transactional
    public AulaDto.Entregas calificar(Long usuarioId, Long entregaId, AulaDto.CalificarRequest req) {
        Entrega en = entregaRepository.findById(entregaId)
                .orElseThrow(() -> new RecursoNoEncontradoException("Entrega no encontrada."));
        Actividad a = en.getActividad();
        accesoService.cursoDelDocente(usuarioId, a.getSeccionCurso().getId());
        if (req == null || req.puntaje() == null) throw new DatosInvalidosException("Escribe el puntaje.");
        double max = a.getPuntajeMaximo();
        if (req.puntaje() < 0 || req.puntaje() > max) {
            throw new DatosInvalidosException("El puntaje debe estar entre 0 y " + formato(max) + ".");
        }
        boolean primera = !Entrega.CALIFICADA.equals(en.getEstado());
        en.setPuntaje(Calificacion.dosDecimales(req.puntaje()));
        en.setComentarioDocente(NotaService.recortar(req.comentario(), 2000));
        en.setEstado(Entrega.CALIFICADA);
        en.setFechaCalificacion(LocalDateTime.now());
        entregaRepository.save(en);
        notaService.registrarDesdeActividad(a, en.getEstudiante(), nota20(en.getPuntaje(), max),
                accesoService.usuario(usuarioId), en.getComentarioDocente());
        Usuario alumno = en.getEstudiante().getUsuario();
        if (primera && alumno != null && Boolean.TRUE.equals(alumno.getEstado())) {
            mensajeService.enviarAviso(List.of(alumno), "Entrega calificada",
                    "<p>Tu entrega de <strong>" + MensajeService.escapar(a.getTitulo()) + "</strong> ("
                            + MensajeService.escapar(a.getSeccionCurso().getCurso().getNombre()) + ") fue calificada: <strong>"
                            + formato(en.getPuntaje()) + " / " + formato(max) + "</strong>.</p>");
        }
        return construirEntregas(a);
    }

    // ==================================================================
    // Armado de respuestas
    // ==================================================================

    private List<AulaDto.CursoAula> cursosDeEstudiante(Estudiante e) {
        Optional<Seccion> s = accesoService.seccionDe(e);
        if (s.isEmpty() || !"Activa".equals(s.get().getEstado())) return List.of();
        return seccionCursoRepository.findBySeccionId(s.get().getId()).stream()
                .filter(sc -> "Activo".equals(sc.getEstado()))
                .sorted(ordenCursos())
                .map(sc -> cursoAula(sc, e))
                .toList();
    }

    private List<AulaDto.ActividadResponse> actividadesDe(Estudiante e) {
        List<Long> ids = cursosDeEstudiante(e).stream().map(AulaDto.CursoAula::seccionCursoId).toList();
        if (ids.isEmpty()) return List.of();
        Map<Long, Set<Long>> ocultasPorCurso = new HashMap<>();
        List<AulaDto.ActividadResponse> lista = actividadRepository.findBySeccionCursoIdInOrderByFechaLimiteAsc(ids).stream()
                .filter(a -> !Boolean.FALSE.equals(a.getVisible()) && !ocultasPorCurso
                        .computeIfAbsent(a.getSeccionCurso().getId(), k -> carpetasOcultas(a.getSeccionCurso()))
                        .contains(a.getCarpetaId()))
                .map(a -> actividadParaAlumno(a, e, false))
                .collect(Collectors.toCollection(ArrayList::new));
        // Pendientes primero (la más próxima arriba); luego las demás de la más reciente a la más antigua.
        lista.sort(Comparator.comparingInt((AulaDto.ActividadResponse r) -> "PENDIENTE".equals(r.situacion()) ? 0 : 1)
                .thenComparing((r1, r2) -> "PENDIENTE".equals(r1.situacion())
                        ? r1.fechaLimite().compareTo(r2.fechaLimite())
                        : r2.fechaLimite().compareTo(r1.fechaLimite())));
        return lista;
    }

    private AulaDto.CursoAula cursoAula(SeccionCurso sc, Estudiante alumno) {
        Seccion s = sc.getSeccion();
        List<Actividad> acts = actividadRepository.findBySeccionCursoIdOrderByFechaLimiteDesc(sc.getId());
        if (alumno != null) acts = soloVisibles(sc, acts);
        long porCalificar = 0, pendientes = 0;
        LocalDateTime ahora = LocalDateTime.now();
        for (Actividad a : acts) {
            if (alumno == null) {
                porCalificar += entregaRepository.countByActividadIdAndEstado(a.getId(), Entrega.ENTREGADA);
            } else if (entregaRepository.findByActividadIdAndEstudianteId(a.getId(), alumno.getId()).isEmpty()
                    && a.getFechaLimite().isAfter(ahora)) {
                pendientes++;
            }
        }
        long materiales;
        if (alumno == null) {
            materiales = materialRepository.countBySeccionCursoId(sc.getId());
        } else {
            Set<Long> ocultas = carpetasOcultas(sc);
            materiales = materialRepository.findBySeccionCursoIdOrderByFechaPublicacionDesc(sc.getId()).stream()
                    .filter(m -> !Boolean.FALSE.equals(m.getVisible()) && !ocultas.contains(m.getCarpetaId())).count();
        }
        CategoriaCurso.Info cat = CategoriaCurso.de(sc.getCurso().getCategoria());
        return new AulaDto.CursoAula(sc.getId(), s.getId(), s.getGrado(), s.getLetra(), s.getTurno(),
                sc.getCurso().getId(), sc.getCurso().getNombre(), cat == null ? "OTROS" : cat.clave(),
                sc.getCurso().getColor(), sc.getDescripcion(),
                sc.getDocente() == null ? null : sc.getDocente().getNombreCompleto(), sc.getEstado(),
                accesoService.alumnosDe(s).size(), materiales, acts.size(), porCalificar, pendientes);
    }

    private AulaDto.MaterialResponse material(Material m) {
        SeccionCurso sc = m.getSeccionCurso();
        String autor = m.getAutorId() == null ? null : usuarioRepository.findById(m.getAutorId()).map(Usuario::getNombre).orElse(null);
        return new AulaDto.MaterialResponse(m.getId(), sc.getId(), sc.getCurso().getNombre(), sc.getCurso().getColor(),
                m.getTitulo(), m.getDescripcion(), m.getEnlace(),
                ArchivoDto.Info.de(m.getArchivoId(), m.getArchivoNombre(), m.getArchivoTipo(), m.getArchivoTamanio()),
                autor, m.getFechaPublicacion(), m.getFechaActualizacion(), tipoMaterial(m),
                !Boolean.FALSE.equals(m.getVisible()), m.getCarpetaId());
    }

    private AulaDto.ActividadResponse actividadParaDocente(Actividad a, boolean conRespuestas) {
        SeccionCurso sc = a.getSeccionCurso();
        long total = accesoService.alumnosDe(sc.getSeccion()).size();
        long entregadas = entregaRepository.countByActividadId(a.getId());
        long calificadas = entregaRepository.countByActividadIdAndEstado(a.getId(), Entrega.CALIFICADA);
        return respuesta(a, preguntas(a, conRespuestas), total, entregadas, calificadas, null, null);
    }

    private AulaDto.ActividadResponse actividadParaAlumno(Actividad a, Estudiante e, boolean detalle) {
        Entrega en = entregaRepository.findByActividadIdAndEstudianteId(a.getId(), e.getId()).orElse(null);
        String situacion;
        if (en != null) situacion = Entrega.CALIFICADA.equals(en.getEstado()) ? "CALIFICADA" : "ENTREGADA";
        else if (LocalDateTime.now().isAfter(a.getFechaLimite())) situacion = "VENCIDA";
        else situacion = "PENDIENTE";
        // Las respuestas correctas se muestran cuando ya no puede volver a intentar.
        // Si se aceptan entregas tardías, no se muestran las soluciones al vencer (otros aún pueden enviar).
        boolean soluciones = en != null && (intentos(en) >= intentosPermitidos(a)
                || (LocalDateTime.now().isAfter(a.getFechaLimite()) && !Boolean.TRUE.equals(a.getPermiteTardia())));
        List<AulaDto.Pregunta> preguntas = detalle ? preguntas(a, soluciones) : List.of();
        return respuesta(a, preguntas, 0, 0, 0, situacion, en == null ? null : entrega(en));
    }

    private AulaDto.ActividadResponse respuesta(Actividad a, List<AulaDto.Pregunta> preguntas, long total,
                                                long entregadas, long calificadas, String situacion,
                                                AulaDto.EntregaResponse miEntrega) {
        SeccionCurso sc = a.getSeccionCurso();
        String periodo = null;
        if (a.getEvaluacionId() != null) {
            periodo = evaluacionRepository.findById(a.getEvaluacionId())
                    .map(ev -> ev.getPeriodo().getNombre()).orElse(null);
        }
        return new AulaDto.ActividadResponse(a.getId(), sc.getId(), sc.getCurso().getNombre(), sc.getCurso().getColor(),
                sc.getSeccion().getGrado(), sc.getSeccion().getLetra(), a.getTipo(), a.getTitulo(), a.getInstrucciones(),
                a.getFechaPublicacion(), a.getFechaLimite(), a.getPuntajeMaximo(), a.getPermiteTardia(), a.getEnlace(),
                ArchivoDto.Info.de(a.getArchivoId(), a.getArchivoNombre(), a.getArchivoTipo(), a.getArchivoTamanio()),
                preguntas, a.getEvaluacionId(), periodo, LocalDateTime.now().isAfter(a.getFechaLimite()),
                total, entregadas, calificadas, situacion, miEntrega,
                !Boolean.FALSE.equals(a.getVisible()), intentosPermitidos(a), a.getCarpetaId(),
                true, formatos(a));
    }

    /** Formatos de archivo que acepta la actividad (todos si el docente no eligió). */
    static List<String> formatos(Actividad a) {
        String f = a.getFormatosPermitidos();
        return ArchivoService.normalizarFormatos(f == null || f.isBlank() ? null : List.of(f.split(",")));
    }

    private List<AulaDto.Pregunta> preguntas(Actividad a, boolean conRespuestas) {
        return a.getPreguntas().stream()
                .map(p -> {
                    boolean multiple = PreguntaCuestionario.MULTIPLE.equals(p.getTipo());
                    Integer correcta = conRespuestas && !multiple ? p.getRespuestaCorrecta() : null;
                    List<Integer> correctas = conRespuestas ? opcionesDe(p, p.getRespuestaCorrecta()) : null;
                    return new AulaDto.Pregunta(p.getId(), p.getOrden(), p.getEnunciado(), new ArrayList<>(p.getOpciones()),
                            correcta, p.getPuntos(), multiple ? PreguntaCuestionario.MULTIPLE : PreguntaCuestionario.UNICA,
                            correctas);
                })
                .toList();
    }

    private AulaDto.EntregaResponse entrega(Entrega en) {
        Actividad a = en.getActividad();
        return new AulaDto.EntregaResponse(en.getId(), a.getId(), en.getEstudiante().getId(),
                en.getEstudiante().getNombreCompleto(), en.getTexto(), en.getEnlace(),
                ArchivoDto.Info.de(en.getArchivoId(), en.getArchivoNombre(), en.getArchivoTipo(), en.getArchivoTamanio()),
                new ArrayList<>(en.getRespuestas()), en.getFechaEntrega(), en.getTardia(), en.getEstado(),
                en.getPuntaje(), a.getPuntajeMaximo(),
                en.getPuntaje() == null ? null : nota20(en.getPuntaje(), a.getPuntajeMaximo()),
                en.getComentarioDocente(), en.getFechaCalificacion(), selecciones(a, en), intentos(en),
                envios(a, en));
    }

    /** Historial "Entrega 1", "Entrega 2"... (las entregas antiguas sin historial muestran solo la actual). */
    private List<AulaDto.Envio> envios(Actividad a, Entrega en) {
        List<AulaDto.Envio> res = new ArrayList<>();
        for (EntregaEnvio v : en.getEnvios()) {
            List<List<Integer>> sel = null;
            if (v.getRespuestas() != null && !v.getRespuestas().isBlank()) {
                List<PreguntaCuestionario> ps = a.getPreguntas();
                String[] partes = v.getRespuestas().split(",");
                sel = new ArrayList<>();
                for (int i = 0; i < partes.length; i++) {
                    int r;
                    try { r = Integer.parseInt(partes[i].trim()); } catch (NumberFormatException ex) { r = -1; }
                    sel.add(i < ps.size() ? opcionesDe(ps.get(i), r) : List.of());
                }
            }
            res.add(new AulaDto.Envio(v.getNumero(), v.getFecha(), v.getTardia(), v.getTexto(), v.getEnlace(),
                    ArchivoDto.Info.de(v.getArchivoId(), v.getArchivoNombre(), v.getArchivoTipo(), v.getArchivoTamanio()),
                    v.getPuntaje(), sel));
        }
        return res;
    }

    private AulaDto.Entregas construirEntregas(Actividad a) {
        Map<Long, Entrega> porAlumno = entregaRepository.findByActividadId(a.getId()).stream()
                .collect(Collectors.toMap(en -> en.getEstudiante().getId(), en -> en, (x, y) -> x));
        LinkedHashMap<Long, AulaDto.FilaEntrega> filas = new LinkedHashMap<>();
        for (Estudiante e : accesoService.alumnosDe(a.getSeccionCurso().getSeccion())) {
            Entrega en = porAlumno.get(e.getId());
            filas.put(e.getId(), new AulaDto.FilaEntrega(e.getId(), e.getNombreCompleto(), en == null ? null : entrega(en), NotaService.foto(e)));
        }
        porAlumno.values().forEach(en -> filas.putIfAbsent(en.getEstudiante().getId(),
                new AulaDto.FilaEntrega(en.getEstudiante().getId(), en.getEstudiante().getNombreCompleto(), entrega(en), NotaService.foto(en.getEstudiante()))));
        return new AulaDto.Entregas(actividadParaDocente(a, true), new ArrayList<>(filas.values()));
    }

    // ==================================================================
    // Validaciones
    // ==================================================================

    private void aplicarMaterial(Material m, AulaDto.MaterialRequest req, Long usuarioId) {
        if (req == null) throw new DatosInvalidosException("Faltan los datos del material.");
        String tipo = req.tipo() == null || req.tipo().isBlank() ? tipoMaterial(m) : req.tipo().trim().toUpperCase();
        if (tipo == null) tipo = "ARCHIVO";
        if (!Set.of("TEXTO", "ARCHIVO", "ENLACE").contains(tipo)) throw new DatosInvalidosException("Tipo de contenido no válido.");
        String titulo = titulo(req.titulo());
        String enlace = enlace(req.enlace());
        Long anterior = m.getArchivoId();
        boolean quitar = Boolean.TRUE.equals(req.quitarArchivo()) || (req.archivo() != null);
        boolean tendraArchivo = req.archivo() != null || (anterior != null && !quitar);
        String descripcion;
        if ("TEXTO".equals(tipo)) {
            descripcion = NotaService.recortar(req.descripcion(), 100000);
            String plano = descripcion == null ? "" : descripcion.replaceAll("<[^>]*>", "").replace("&nbsp;", " ").trim();
            if (plano.isEmpty()) throw new DatosInvalidosException("Escribe el contenido del texto.");
            if (req.archivo() != null && (req.archivo().tipo() == null || !req.archivo().tipo().startsWith("image/"))) {
                throw new DatosInvalidosException("En un texto solo puedes adjuntar una imagen (JPG o PNG).");
            }
        } else {
            descripcion = NotaService.recortar(req.descripcion(), 5000);
            if ("ARCHIVO".equals(tipo) && !tendraArchivo) throw new DatosInvalidosException("Selecciona el archivo.");
            if ("ENLACE".equals(tipo) && enlace == null) throw new DatosInvalidosException("Escribe el enlace.");
        }
        ArchivoAdjunto nuevo = archivoService.guardar(req.archivo(), usuarioId);
        m.setTipo(tipo);
        m.setTitulo(titulo);
        m.setDescripcion(descripcion);
        m.setEnlace(enlace);
        if (quitar && anterior != null) {
            m.setArchivoId(null);
            m.setArchivoNombre(null);
            m.setArchivoTipo(null);
            m.setArchivoTamanio(null);
            archivoService.eliminar(anterior);
        }
        if (nuevo != null) {
            m.setArchivoId(nuevo.getId());
            m.setArchivoNombre(nuevo.getNombre());
            m.setArchivoTipo(nuevo.getTipoMime());
            m.setArchivoTamanio(nuevo.getTamanio());
        }
    }

    /** puedeCambiarPreguntas = false cuando ya hay entregas (no se altera lo que los alumnos respondieron). */
    private void aplicarActividad(Actividad a, AulaDto.ActividadRequest req, Long usuarioId, boolean puedeCambiarPreguntas) {
        if (req == null) throw new DatosInvalidosException("Faltan los datos de la actividad.");
        String tipo = req.tipo() == null ? "" : req.tipo().trim().toUpperCase();
        if (!TIPOS.contains(tipo)) throw new DatosInvalidosException("Elige el tipo: tarea, cuestionario o actividad.");
        if (a.getId() != null && !tipo.equals(a.getTipo()) && !puedeCambiarPreguntas) {
            throw new DatosInvalidosException("No se puede cambiar el tipo porque ya hay entregas.");
        }
        if (req.fechaLimite() == null) throw new DatosInvalidosException("Indica la fecha y hora límite.");
        if (a.getId() == null && req.fechaLimite().isBefore(LocalDateTime.now())) {
            throw new DatosInvalidosException("La fecha límite debe ser posterior a este momento.");
        }
        a.setTipo(tipo);
        a.setTitulo(titulo(req.titulo()));
        a.setInstrucciones(NotaService.recortar(req.instrucciones(), 20000));
        a.setFechaLimite(req.fechaLimite());
        // Opción del docente: aceptar entregas después de la fecha límite (quedan marcadas como tardías).
        a.setPermiteTardia(Boolean.TRUE.equals(req.permiteTardia()));
        a.setFormatosPermitidos(String.join(",", ArchivoService.normalizarFormatos(req.formatosPermitidos())));
        int intentos = req.intentosPermitidos() == null ? 1 : req.intentosPermitidos();
        if (intentos < 1 || intentos > 5) throw new DatosInvalidosException("Los intentos permitidos van de 1 a 5.");
        a.setIntentosPermitidos(intentos);
        a.setEnlace(enlace(req.enlace()));

        ArchivoAdjunto nuevo = archivoService.guardar(req.archivo(), usuarioId);
        Long anterior = a.getArchivoId();
        if ((Boolean.TRUE.equals(req.quitarArchivo()) || nuevo != null) && anterior != null) {
            a.setArchivoId(null);
            a.setArchivoNombre(null);
            a.setArchivoTipo(null);
            a.setArchivoTamanio(null);
            archivoService.eliminar(anterior);
        }
        if (nuevo != null) {
            a.setArchivoId(nuevo.getId());
            a.setArchivoNombre(nuevo.getNombre());
            a.setArchivoTipo(nuevo.getTipoMime());
            a.setArchivoTamanio(nuevo.getTamanio());
        }

        if (Actividad.CUESTIONARIO.equals(tipo)) {
            if (puedeCambiarPreguntas) {
                List<AulaDto.PreguntaRequest> preguntas = req.preguntas() == null ? List.of() : req.preguntas();
                if (preguntas.isEmpty()) throw new DatosInvalidosException("Agrega al menos una pregunta al cuestionario.");
                if (preguntas.size() > 50) throw new DatosInvalidosException("El cuestionario admite hasta 50 preguntas.");
                a.getPreguntas().clear();
                double total = 0;
                int orden = 1;
                for (AulaDto.PreguntaRequest p : preguntas) {
                    String n = "Pregunta " + orden + ": ";
                    String enunciado = p == null ? null : NotaService.recortar(p.enunciado(), 2000);
                    if (enunciado == null) throw new DatosInvalidosException(n + "escribe el enunciado.");
                    List<String> opciones = new ArrayList<>();
                    for (String o : p.opciones() == null ? List.<String>of() : p.opciones()) {
                        String t = NotaService.recortar(o, 500);
                        if (t != null) opciones.add(t);
                    }
                    if (opciones.size() < 2) throw new DatosInvalidosException(n + "escribe al menos 2 opciones.");
                    if (opciones.size() > 8) throw new DatosInvalidosException(n + "máximo 8 opciones.");
                    boolean multiple = PreguntaCuestionario.MULTIPLE.equalsIgnoreCase(p.tipo() == null ? "" : p.tipo().trim());
                    Integer correcta;
                    if (multiple) {
                        int mascara = 0;
                        for (Integer i : p.respuestasCorrectas() == null ? List.<Integer>of() : p.respuestasCorrectas()) {
                            if (i == null || i < 0 || i >= opciones.size()) throw new DatosInvalidosException(n + "opción correcta no válida.");
                            mascara |= 1 << i;
                        }
                        if (mascara == 0) throw new DatosInvalidosException(n + "marca al menos una respuesta correcta.");
                        correcta = mascara;
                    } else {
                        correcta = p.respuestaCorrecta();
                        if (correcta == null && p.respuestasCorrectas() != null && p.respuestasCorrectas().size() == 1) {
                            correcta = p.respuestasCorrectas().get(0);
                        }
                        if (correcta == null || correcta < 0 || correcta >= opciones.size()) {
                            throw new DatosInvalidosException(n + "marca la respuesta correcta.");
                        }
                    }
                    double puntos = p.puntos() == null ? 1 : p.puntos();
                    if (puntos <= 0 || puntos > 20) throw new DatosInvalidosException(n + "los puntos van de 0.5 a 20.");
                    total += puntos;
                    a.getPreguntas().add(PreguntaCuestionario.builder().actividad(a).orden(orden++)
                            .enunciado(enunciado).opciones(opciones).respuestaCorrecta(correcta)
                            .tipo(multiple ? PreguntaCuestionario.MULTIPLE : PreguntaCuestionario.UNICA)
                            .puntos(Calificacion.dosDecimales(puntos)).build());
                }
                a.setPuntajeMaximo(Calificacion.dosDecimales(total));
            }
        } else {
            a.getPreguntas().clear();
            double max = req.puntajeMaximo() == null ? 20 : req.puntajeMaximo();
            if (max <= 0 || max > 100) throw new DatosInvalidosException("El puntaje máximo debe estar entre 1 y 100.");
            if (!puedeCambiarPreguntas && !Objects.equals(a.getPuntajeMaximo(), max)
                    && entregaRepository.countByActividadIdAndEstado(a.getId(), Entrega.CALIFICADA) > 0) {
                throw new DatosInvalidosException("No se puede cambiar el puntaje máximo porque ya hay entregas calificadas.");
            }
            a.setPuntajeMaximo(Calificacion.dosDecimales(max));
        }
    }

    private static String titulo(String t) {
        String v = t == null ? "" : t.trim().replaceAll("\\s+", " ");
        if (v.isEmpty()) throw new DatosInvalidosException("Escribe el título.");
        if (v.length() > 200) throw new DatosInvalidosException("El título admite hasta 200 caracteres.");
        return v;
    }

    private static String enlace(String e) {
        String v = e == null ? "" : e.trim();
        if (v.isEmpty()) return null;
        if (!v.matches("(?i)^https?://\\S+$")) throw new DatosInvalidosException("El enlace debe empezar con http:// o https://");
        if (v.length() > 500) throw new DatosInvalidosException("El enlace es demasiado largo.");
        return v;
    }

    private static void ponerArchivo(Entrega en, ArchivoAdjunto a) {
        en.setArchivoId(a.getId());
        en.setArchivoNombre(a.getNombre());
        en.setArchivoTipo(a.getTipoMime());
        en.setArchivoTamanio(a.getTamanio());
    }

    // ------------------------------------------------------------------
    // Ayudantes: visibilidad, carpetas, intentos y cuestionarios
    // ------------------------------------------------------------------

    /** El docente del curso y la administración ven también lo oculto. */
    private boolean veTodo(Long usuarioId, SeccionCurso sc) {
        Usuario u = accesoService.usuario(usuarioId);
        return AccesoService.esDocenteDe(usuarioId, sc) || AccesoService.esAdministrativo(u);
    }

    private Set<Long> carpetasOcultas(SeccionCurso sc) {
        return carpetaRepository.findBySeccionCursoIdOrderByFechaCreacionAsc(sc.getId()).stream()
                .filter(c -> Boolean.FALSE.equals(c.getVisible())).map(Carpeta::getId).collect(Collectors.toSet());
    }

    /** Solo las actividades visibles y fuera de carpetas ocultas. */
    private List<Actividad> soloVisibles(SeccionCurso sc, List<Actividad> lista) {
        Set<Long> ocultas = carpetasOcultas(sc);
        return lista.stream().filter(a -> !Boolean.FALSE.equals(a.getVisible()) && !ocultas.contains(a.getCarpetaId())).toList();
    }

    private Long carpetaDelCurso(SeccionCurso sc, Long carpetaId) {
        if (carpetaId == null) return null;
        Carpeta c = buscarCarpeta(carpetaId);
        if (!c.getSeccionCurso().getId().equals(sc.getId())) throw new DatosInvalidosException("La carpeta no es de este curso.");
        return c.getId();
    }

    private Carpeta buscarCarpeta(Long id) {
        return carpetaRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Carpeta no encontrada."));
    }

    private static String nombreCarpeta(String n) {
        String v = n == null ? "" : n.trim().replaceAll("\\s+", " ");
        if (v.isEmpty()) throw new DatosInvalidosException("Escribe el nombre de la carpeta.");
        if (v.length() > 120) throw new DatosInvalidosException("El nombre admite hasta 120 caracteres.");
        return v;
    }

    private static AulaDto.Carpeta carpeta(Carpeta c) {
        Periodo p = c.getPeriodo();
        return new AulaDto.Carpeta(c.getId(), c.getSeccionCurso().getId(), p.getId(), p.getNombre(), p.getBimestre(),
                p.getAnio(), p.getFechaInicio(), p.getFechaFin(), c.getNombre(), !Boolean.FALSE.equals(c.getVisible()),
                c.getFechaCreacion());
    }

    /** Materiales antiguos (sin tipo): archivo, enlace o texto según lo que tengan. */
    private static String tipoMaterial(Material m) {
        if (m.getTipo() != null) return m.getTipo();
        if (m.getArchivoId() != null) return "ARCHIVO";
        if (m.getEnlace() != null) return "ENLACE";
        return m.getId() == null ? null : "TEXTO";
    }

    static int intentosPermitidos(Actividad a) {
        return a.getIntentosPermitidos() == null || a.getIntentosPermitidos() < 1 ? 1 : a.getIntentosPermitidos();
    }

    static int intentos(Entrega en) {
        return en.getIntentos() == null ? 1 : en.getIntentos();
    }

    /** Opciones marcadas a partir del valor guardado (índice o máscara de bits). */
    private static List<Integer> opcionesDe(PreguntaCuestionario p, Integer valor) {
        List<Integer> res = new ArrayList<>();
        if (valor == null || valor < 0) return res;
        if (PreguntaCuestionario.MULTIPLE.equals(p.getTipo())) {
            for (int i = 0; i < p.getOpciones().size(); i++) if ((valor & (1 << i)) != 0) res.add(i);
        } else {
            res.add(valor);
        }
        return res;
    }

    private static List<List<Integer>> selecciones(Actividad a, Entrega en) {
        List<List<Integer>> res = new ArrayList<>();
        List<PreguntaCuestionario> ps = a.getPreguntas();
        for (int i = 0; i < en.getRespuestas().size(); i++) {
            res.add(i < ps.size() ? opcionesDe(ps.get(i), en.getRespuestas().get(i)) : List.of());
        }
        return res;
    }

    /** Convierte lo enviado (selecciones o respuestas) a lo que se guarda: índice (UNICA) o máscara (MULTIPLE). */
    private static List<Integer> respuestasCuestionario(List<PreguntaCuestionario> preguntas, AulaDto.EntregaRequest req) {
        List<Integer> res = new ArrayList<>();
        for (int i = 0; i < preguntas.size(); i++) {
            PreguntaCuestionario p = preguntas.get(i);
            List<Integer> marcadas;
            if (req.selecciones() != null && i < req.selecciones().size() && req.selecciones().get(i) != null) {
                marcadas = req.selecciones().get(i);
            } else if (req.respuestas() != null && i < req.respuestas().size() && req.respuestas().get(i) != null) {
                marcadas = List.of(req.respuestas().get(i));
            } else {
                marcadas = List.of();
            }
            if (marcadas.isEmpty()) throw new DatosInvalidosException("Falta responder la pregunta " + (i + 1) + ".");
            if (PreguntaCuestionario.MULTIPLE.equals(p.getTipo())) {
                int mascara = 0;
                for (Integer k : marcadas) {
                    if (k == null || k < 0 || k >= p.getOpciones().size()) throw new DatosInvalidosException("Respuesta no válida en la pregunta " + (i + 1) + ".");
                    mascara |= 1 << k;
                }
                res.add(mascara);
            } else {
                Integer k = marcadas.get(0);
                if (marcadas.size() > 1 || k == null || k < 0 || k >= p.getOpciones().size()) {
                    throw new DatosInvalidosException("En la pregunta " + (i + 1) + " marca una sola opción.");
                }
                res.add(k);
            }
        }
        return res;
    }

    private Actividad buscarActividad(Long id) {
        return actividadRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Actividad no encontrada."));
    }

    private Usuario docenteUsuario(SeccionCurso sc) {
        if (sc.getDocente() == null || sc.getDocente().getUsuarioId() == null) return null;
        return usuarioRepository.findById(sc.getDocente().getUsuarioId()).orElse(null);
    }

    /** Aviso del colegio a los alumnos de la sección. */
    private void avisarSeccion(SeccionCurso sc, String asunto, String html) {
        List<Usuario> alumnos = accesoService.cuentasAlumnos(sc.getSeccion());
        if (!alumnos.isEmpty()) mensajeService.enviarAviso(alumnos, asunto, html);
    }

    static Double nota20(Double puntaje, Double maximo) {
        if (puntaje == null || maximo == null || maximo <= 0) return null;
        return Calificacion.dosDecimales(Math.min(20, puntaje * 20 / maximo));
    }

    static String etiquetaTipo(String tipo) {
        return switch (tipo) {
            case Actividad.CUESTIONARIO -> "Cuestionario";
            case Actividad.ACTIVIDAD -> "Actividad";
            default -> "Tarea";
        };
    }

    private static String formato(double v) {
        return v == Math.floor(v) ? String.valueOf((long) v) : String.valueOf(Calificacion.dosDecimales(v));
    }

    private static Comparator<SeccionCurso> ordenCursos() {
        return Comparator.comparingInt((SeccionCurso sc) -> Grados.orden(sc.getSeccion().getGrado()))
                .thenComparing(sc -> sc.getSeccion().getLetra())
                .thenComparing(sc -> sc.getCurso().getNombre(), String.CASE_INSENSITIVE_ORDER);
    }
}
