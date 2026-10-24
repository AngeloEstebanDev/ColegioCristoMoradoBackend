package com.CristoMorado_api.CristoMorado_api.services;

import com.CristoMorado_api.CristoMorado_api.dto.NotaDto;
import com.CristoMorado_api.CristoMorado_api.entity.*;
import com.CristoMorado_api.CristoMorado_api.exception.AccesoDenegadoException;
import com.CristoMorado_api.CristoMorado_api.exception.DatosInvalidosException;
import com.CristoMorado_api.CristoMorado_api.exception.RecursoNoEncontradoException;
import com.CristoMorado_api.CristoMorado_api.repository.*;
import com.CristoMorado_api.CristoMorado_api.util.Calificacion;
import com.CristoMorado_api.CristoMorado_api.util.CategoriaCurso;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * HU-16 Registro de calificaciones, HU-17 Actualización (con historial de cambios),
 * HU-19 Promedios automáticos, HU-20 Consulta detallada y HU-21 Historial académico.
 * Notas vigesimales (0 a 20), organizadas por curso de sección y periodo (bimestre).
 *
 * Las tareas, cuestionarios y actividades del aula NO salen una por una en la libreta:
 * se juntan en una sola nota "Participación" (su promedio). Junto a ella van los exámenes
 * y registros que el docente crea a mano (Examen Mensual, Examen Bimestral, Comportamiento...).
 */
@Service
public class NotaService {

    private final EvaluacionRepository evaluacionRepository;
    private final NotaRepository notaRepository;
    private final NotaHistorialRepository historialRepository;
    private final PeriodoRepository periodoRepository;
    private final EstudianteRepository estudianteRepository;
    private final ActividadRepository actividadRepository;
    private final SeccionCursoRepository seccionCursoRepository;
    private final AsistenciaRepository asistenciaRepository;
    private final AccesoService accesoService;
    private final EntregaRepository entregaRepository;

    public NotaService(EvaluacionRepository evaluacionRepository, NotaRepository notaRepository,
                       NotaHistorialRepository historialRepository, PeriodoRepository periodoRepository,
                       EstudianteRepository estudianteRepository, ActividadRepository actividadRepository,
                       SeccionCursoRepository seccionCursoRepository, AsistenciaRepository asistenciaRepository,
                       AccesoService accesoService, EntregaRepository entregaRepository) {
        this.evaluacionRepository = evaluacionRepository;
        this.notaRepository = notaRepository;
        this.historialRepository = historialRepository;
        this.periodoRepository = periodoRepository;
        this.estudianteRepository = estudianteRepository;
        this.actividadRepository = actividadRepository;
        this.seccionCursoRepository = seccionCursoRepository;
        this.asistenciaRepository = asistenciaRepository;
        this.accesoService = accesoService;
        this.entregaRepository = entregaRepository;
    }

    // ==================================================================
    // Registro de notas del docente (HU-16 / HU-17 / HU-19)
    // ==================================================================

    /** El docente del curso (edita) o la administración (solo consulta). */
    @Transactional(readOnly = true)
    public NotaDto.Registro registro(Long usuarioId, Long seccionCursoId, Long periodoId) {
        SeccionCurso sc = cursoParaLeer(usuarioId, seccionCursoId);
        return construirRegistro(sc, elegirPeriodo(periodoId));
    }

    @Transactional
    public NotaDto.Registro crearEvaluacion(Long usuarioId, Long seccionCursoId, NotaDto.EvaluacionRequest req) {
        SeccionCurso sc = accesoService.cursoDelDocente(usuarioId, seccionCursoId);
        if (req == null) throw new DatosInvalidosException("Faltan los datos de la evaluación.");
        Periodo p = req.periodoId() == null ? periodoActivo()
                : periodoRepository.findById(req.periodoId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Periodo no encontrado."));
        if (p == null) throw new DatosInvalidosException("No hay un periodo activo. Pide a la administración que active el bimestre.");
        Evaluacion ev = Evaluacion.builder().seccionCurso(sc).periodo(p).build();
        aplicar(ev, req);
        evaluacionRepository.save(ev);
        return construirRegistro(sc, p);
    }

    @Transactional
    public NotaDto.Registro actualizarEvaluacion(Long usuarioId, Long evaluacionId, NotaDto.EvaluacionRequest req) {
        Evaluacion ev = evaluacion(evaluacionId);
        accesoService.cursoDelDocente(usuarioId, ev.getSeccionCurso().getId());
        if (req == null) throw new DatosInvalidosException("Faltan los datos de la evaluación.");
        aplicar(ev, req);
        evaluacionRepository.save(ev);
        if (ev.getActividadId() != null) {
            actividadRepository.findById(ev.getActividadId()).ifPresent(a -> {
                a.setTitulo(ev.getNombre());
                actividadRepository.save(a);
            });
        }
        return construirRegistro(ev.getSeccionCurso(), ev.getPeriodo());
    }

    /** Borra la columna y sus notas. Si venía de una actividad, la actividad queda sin vincular. */
    @Transactional
    public NotaDto.Registro eliminarEvaluacion(Long usuarioId, Long evaluacionId) {
        Evaluacion ev = evaluacion(evaluacionId);
        accesoService.cursoDelDocente(usuarioId, ev.getSeccionCurso().getId());
        Usuario u = accesoService.usuario(usuarioId);
        for (Nota n : notaRepository.findByEvaluacionId(ev.getId())) {
            historial(ev.getId(), n.getEstudiante().getId(), n.getValor(), null, "Se eliminó la evaluación " + ev.getNombre(), u);
            notaRepository.delete(n);
        }
        if (ev.getActividadId() != null) {
            actividadRepository.findById(ev.getActividadId()).ifPresent(a -> {
                a.setEvaluacionId(null);
                actividadRepository.save(a);
            });
        }
        SeccionCurso sc = ev.getSeccionCurso();
        Periodo p = ev.getPeriodo();
        evaluacionRepository.delete(ev);
        evaluacionRepository.flush();
        return construirRegistro(sc, p);
    }

    /** Guarda varias notas a la vez. Cada modificación queda en el historial (HU-17). */
    @Transactional
    public NotaDto.Registro guardarNotas(Long usuarioId, Long seccionCursoId, NotaDto.GuardarNotasRequest req) {
        SeccionCurso sc = accesoService.cursoDelDocente(usuarioId, seccionCursoId);
        Usuario u = accesoService.usuario(usuarioId);
        if (req == null || req.cambios() == null || req.cambios().isEmpty()) {
            throw new DatosInvalidosException("No hay cambios para guardar.");
        }
        String motivo = recortar(req.motivo(), 300);
        Periodo periodo = null;
        for (NotaDto.Cambio c : req.cambios()) {
            if (c == null || c.evaluacionId() == null || c.estudianteId() == null) {
                throw new DatosInvalidosException("Cada nota debe indicar la evaluación y el alumno.");
            }
            Evaluacion ev = evaluacion(c.evaluacionId());
            if (!ev.getSeccionCurso().getId().equals(sc.getId())) {
                throw new DatosInvalidosException("La evaluación no pertenece a este curso.");
            }
            periodo = ev.getPeriodo();
            Estudiante e = estudianteRepository.findById(c.estudianteId())
                    .orElseThrow(() -> new RecursoNoEncontradoException("Alumno no encontrado."));
            Optional<Nota> actual = notaRepository.findByEvaluacionIdAndEstudianteId(ev.getId(), e.getId());
            if (actual.isEmpty() && !AccesoService.estaEn(e, sc.getSeccion())) {
                throw new DatosInvalidosException(e.getNombreCompleto() + " no está matriculado en esta sección.");
            }
            guardarNota(ev, e, c.valor(), recortar(c.comentario(), 300), u, motivo, actual.orElse(null));
        }
        return construirRegistro(sc, periodo);
    }

    @Transactional(readOnly = true)
    public List<NotaDto.CambioHistorial> historialCurso(Long usuarioId, Long seccionCursoId, Long periodoId) {
        SeccionCurso sc = cursoParaLeer(usuarioId, seccionCursoId);
        Periodo p = elegirPeriodo(periodoId);
        List<Evaluacion> evs = p == null ? evaluacionRepository.findBySeccionCursoId(sc.getId())
                : evaluacionRepository.findBySeccionCursoIdAndPeriodoIdOrderByFechaRegistroAsc(sc.getId(), p.getId());
        if (evs.isEmpty()) return List.of();
        Map<Long, String> nombresEv = evs.stream().collect(Collectors.toMap(Evaluacion::getId, Evaluacion::getNombre));
        Map<Long, String> alumnos = new HashMap<>();
        return historialRepository.findByEvaluacionIdInOrderByFechaDesc(nombresEv.keySet()).stream()
                .limit(300)
                .map(h -> new NotaDto.CambioHistorial(h.getId(), nombresEv.get(h.getEvaluacionId()),
                        alumnos.computeIfAbsent(h.getEstudianteId(), id -> estudianteRepository.findById(id)
                                .map(Estudiante::getNombreCompleto).orElse("Alumno eliminado")),
                        h.getValorAnterior(), h.getValorNuevo(), h.getMotivo(), h.getUsuarioNombre(), h.getFecha()))
                .toList();
    }

    /** Participación del curso en un periodo: promedio y detalle por alumno. */
    @Transactional(readOnly = true)
    public NotaDto.Participacion participacion(Long usuarioId, Long seccionCursoId, Long periodoId) {
        SeccionCurso sc = cursoParaLeer(usuarioId, seccionCursoId);
        Periodo p = elegirPeriodo(periodoId);
        if (p == null) return new NotaDto.Participacion(sc.getId(), null, null, 0, List.of());
        List<Evaluacion> evs = evaluacionRepository.findBySeccionCursoIdAndPeriodoIdOrderByFechaRegistroAsc(sc.getId(), p.getId());
        List<Estudiante> alumnos = accesoService.alumnosDe(sc.getSeccion());
        CalculoParticipacion c = calcularParticipacion(evs, alumnos);
        List<NotaDto.ParticipacionAlumno> filas = alumnos.stream()
                .map(e -> new NotaDto.ParticipacionAlumno(e.getId(), e.getNombreCompleto(), foto(e),
                        Calificacion.promedio(c.promedios().get(e.getId())), c.detalle().getOrDefault(e.getId(), List.of())))
                .toList();
        return new NotaDto.Participacion(sc.getId(), corto(p), c.fechaRegistro(), c.actividades().size(), filas);
    }

    // ==================================================================
    // Integración con actividades (la nota de una entrega pasa al registro)
    // ==================================================================

    /** Evaluación del registro vinculada a la actividad; la crea en el periodo activo si falta. */
    @Transactional
    public Evaluacion asegurarEvaluacion(Actividad a) {
        if (a.getEvaluacionId() != null) {
            Optional<Evaluacion> ev = evaluacionRepository.findById(a.getEvaluacionId());
            if (ev.isPresent()) return ev.get();
        }
        Periodo p = periodoActivo();
        if (p == null) return null;
        Evaluacion ev = evaluacionRepository.save(Evaluacion.builder()
                .seccionCurso(a.getSeccionCurso()).periodo(p)
                .nombre(recortar(a.getTitulo(), 120))
                .tipo(Calificacion.TIPOS.contains(a.getTipo()) ? a.getTipo() : "TAREA")
                .peso(1)
                .fecha(a.getFechaLimite() == null ? LocalDate.now() : a.getFechaLimite().toLocalDate())
                .actividadId(a.getId())
                .build());
        a.setEvaluacionId(ev.getId());
        actividadRepository.save(a);
        return ev;
    }

    /** Pasa la nota (ya en escala 0-20) de una entrega calificada al registro. */
    @Transactional
    public void registrarDesdeActividad(Actividad a, Estudiante e, Double nota20, Usuario docente, String comentario) {
        Evaluacion ev = asegurarEvaluacion(a);
        if (ev == null || nota20 == null) return;
        Nota actual = notaRepository.findByEvaluacionIdAndEstudianteId(ev.getId(), e.getId()).orElse(null);
        guardarNota(ev, e, Calificacion.validar(nota20), recortar(comentario, 300), docente,
                "Calificación de la actividad \"" + a.getTitulo() + "\"", actual);
    }

    /** Actualiza nombre/fecha de la evaluación cuando el docente edita la actividad. */
    @Transactional
    public void sincronizarEvaluacion(Actividad a) {
        if (a.getEvaluacionId() == null) return;
        evaluacionRepository.findById(a.getEvaluacionId()).ifPresent(ev -> {
            ev.setNombre(recortar(a.getTitulo(), 120));
            if (a.getFechaLimite() != null) ev.setFecha(a.getFechaLimite().toLocalDate());
            evaluacionRepository.save(ev);
        });
    }

    /** Al borrar una actividad se borra su columna de notas. */
    @Transactional
    public void eliminarEvaluacionDeActividad(Actividad a, Usuario u) {
        if (a.getEvaluacionId() == null) return;
        evaluacionRepository.findById(a.getEvaluacionId()).ifPresent(ev -> {
            for (Nota n : notaRepository.findByEvaluacionId(ev.getId())) {
                historial(ev.getId(), n.getEstudiante().getId(), n.getValor(), null,
                        "Se eliminó la actividad " + a.getTitulo(), u);
                notaRepository.delete(n);
            }
            evaluacionRepository.delete(ev);
        });
    }

    // ==================================================================
    // Boleta del alumno (HU-20) e historial académico (HU-21)
    // ==================================================================

    @Transactional(readOnly = true)
    public NotaDto.Boleta boleta(Long usuarioId, Long estudianteId, Integer anio) {
        Estudiante e = accesoService.estudianteVisible(usuarioId, estudianteId);
        return construirBoleta(e, anio);
    }

    @Transactional(readOnly = true)
    public NotaDto.Boleta miBoleta(Long usuarioId, Integer anio) {
        return construirBoleta(accesoService.alumnoLogeado(usuarioId), anio);
    }

    @Transactional(readOnly = true)
    public NotaDto.Historial historial(Long usuarioId, Long estudianteId) {
        return construirHistorial(accesoService.estudianteVisible(usuarioId, estudianteId));
    }

    @Transactional(readOnly = true)
    public NotaDto.Historial miHistorial(Long usuarioId) {
        return construirHistorial(accesoService.alumnoLogeado(usuarioId));
    }

    /** Promedio del alumno en un curso y periodo (lo usan los reportes). */
    @Transactional(readOnly = true)
    public Map<Long, Double> promediosDelCurso(SeccionCurso sc, Periodo p) {
        List<Evaluacion> evs = evaluacionRepository.findBySeccionCursoIdAndPeriodoIdOrderByFechaRegistroAsc(sc.getId(), p.getId());
        return promediosPorAlumno(evs, accesoService.alumnosDe(sc.getSeccion()));
    }

    // ------------------------------------------------------------------

    private NotaDto.Registro construirRegistro(SeccionCurso sc, Periodo p) {
        Seccion s = sc.getSeccion();
        List<NotaDto.PeriodoCorto> periodos = periodosOrdenados().stream().map(NotaService::corto).toList();
        if (p == null) {
            return new NotaDto.Registro(sc.getId(), sc.getCurso().getNombre(), sc.getCurso().getColor(), s.getGrado(),
                    s.getLetra(), null, periodos, List.of(), filasSinNotas(s), Calificacion.promedio(null), 0, 0, 0);
        }
        List<Evaluacion> evs = evaluacionRepository.findBySeccionCursoIdAndPeriodoIdOrderByFechaRegistroAsc(sc.getId(), p.getId());
        evs = new ArrayList<>(evs);
        evs.sort(Comparator.comparing((Evaluacion ev) -> ev.getFecha() == null ? LocalDate.MAX : ev.getFecha())
                .thenComparing(Evaluacion::getId));
        // Columnas: solo los exámenes/registros del docente; las actividades van dentro de Participación.
        List<Evaluacion> manuales = evs.stream().filter(ev -> ev.getActividadId() == null).toList();
        List<Nota> notas = manuales.isEmpty() ? List.of()
                : notaRepository.findByEvaluacionIdIn(manuales.stream().map(Evaluacion::getId).toList());
        Map<Long, Integer> pesos = manuales.stream().collect(Collectors.toMap(Evaluacion::getId, Evaluacion::getPeso));
        Map<Long, Long> conteo = notas.stream().collect(Collectors.groupingBy(n -> n.getEvaluacion().getId(), Collectors.counting()));

        // Alumnos de la sección + los que tienen notas aunque ya no estén en ella.
        LinkedHashMap<Long, Estudiante> alumnos = new LinkedHashMap<>();
        accesoService.alumnosDe(s).forEach(e -> alumnos.put(e.getId(), e));
        notas.forEach(n -> alumnos.putIfAbsent(n.getEstudiante().getId(), n.getEstudiante()));

        Map<Long, List<Nota>> porAlumno = notas.stream().collect(Collectors.groupingBy(n -> n.getEstudiante().getId()));
        CalculoParticipacion part = calcularParticipacion(evs, alumnos.values());
        List<NotaDto.FilaAlumno> filas = new ArrayList<>();
        long aprobados = 0, desaprobados = 0, sinNotas = 0;
        List<Double> promedios = new ArrayList<>();
        for (Estudiante e : alumnos.values()) {
            Map<Long, Double> valores = new LinkedHashMap<>();
            Map<Long, String> comentarios = new LinkedHashMap<>();
            List<Calificacion.Ponderada> pond = new ArrayList<>();
            for (Nota n : porAlumno.getOrDefault(e.getId(), List.of())) {
                valores.put(n.getEvaluacion().getId(), n.getValor());
                if (n.getComentario() != null) comentarios.put(n.getEvaluacion().getId(), n.getComentario());
                pond.add(new Calificacion.Ponderada(n.getValor(), pesos.getOrDefault(n.getEvaluacion().getId(), 1)));
            }
            Double participacion = part.promedios().get(e.getId());
            if (participacion != null) pond.add(new Calificacion.Ponderada(participacion, 1));
            Double prom = Calificacion.ponderado(pond);
            if (prom == null) sinNotas++;
            else if (Calificacion.aprobado(prom)) aprobados++;
            else desaprobados++;
            promedios.add(prom);
            filas.add(new NotaDto.FilaAlumno(e.getId(), e.getNombreCompleto(), valores, comentarios, Calificacion.promedio(prom),
                    participacion, foto(e)));
        }
        filas.sort(Comparator.comparing(NotaDto.FilaAlumno::nombre, String.CASE_INSENSITIVE_ORDER));

        List<NotaDto.Evaluacion> columnas = manuales.stream()
                .map(ev -> new NotaDto.Evaluacion(ev.getId(), ev.getNombre(), ev.getTipo(), ev.getPeso(), ev.getFecha(),
                        ev.getActividadId(), conteo.getOrDefault(ev.getId(), 0L)))
                .toList();
        return new NotaDto.Registro(sc.getId(), sc.getCurso().getNombre(), sc.getCurso().getColor(), s.getGrado(),
                s.getLetra(), corto(p), periodos, columnas, filas, Calificacion.promedio(Calificacion.simple(promedios)),
                aprobados, desaprobados, sinNotas);
    }

    private List<NotaDto.FilaAlumno> filasSinNotas(Seccion s) {
        return accesoService.alumnosDe(s).stream()
                .map(e -> new NotaDto.FilaAlumno(e.getId(), e.getNombreCompleto(), Map.of(), Map.of(), Calificacion.promedio(null),
                        null, foto(e)))
                .toList();
    }

    private Map<Long, Double> promediosPorAlumno(List<Evaluacion> evs, Collection<Estudiante> alumnos) {
        if (evs.isEmpty()) return Map.of();
        Map<Long, Integer> pesos = evs.stream().filter(ev -> ev.getActividadId() == null)
                .collect(Collectors.toMap(Evaluacion::getId, Evaluacion::getPeso));
        Map<Long, List<Calificacion.Ponderada>> porAlumno = new HashMap<>();
        if (!pesos.isEmpty()) {
            for (Nota n : notaRepository.findByEvaluacionIdIn(pesos.keySet())) {
                porAlumno.computeIfAbsent(n.getEstudiante().getId(), k -> new ArrayList<>())
                        .add(new Calificacion.Ponderada(n.getValor(), pesos.getOrDefault(n.getEvaluacion().getId(), 1)));
            }
        }
        calcularParticipacion(evs, alumnos).promedios().forEach((id, v) -> {
            if (v != null) porAlumno.computeIfAbsent(id, k -> new ArrayList<>()).add(new Calificacion.Ponderada(v, 1));
        });
        Map<Long, Double> res = new HashMap<>();
        porAlumno.forEach((id, lista) -> res.put(id, Calificacion.ponderado(lista)));
        return res;
    }

    private void guardarNota(Evaluacion ev, Estudiante e, Double valor, String comentario, Usuario u,
                             String motivo, Nota actual) {
        if (valor == null) {
            if (actual != null) {
                historial(ev.getId(), e.getId(), actual.getValor(), null, motivo, u);
                notaRepository.delete(actual);
            }
            return;
        }
        Double v = Calificacion.validar(valor);
        if (actual == null) {
            notaRepository.save(Nota.builder().evaluacion(ev).estudiante(e).valor(v).comentario(comentario)
                    .registradoPor(u.getId()).build());
            historial(ev.getId(), e.getId(), null, v, motivo, u);
            return;
        }
        boolean cambioValor = !Objects.equals(actual.getValor(), v);
        boolean cambioComentario = !Objects.equals(actual.getComentario(), comentario);
        if (!cambioValor && !cambioComentario) return;
        if (cambioValor) historial(ev.getId(), e.getId(), actual.getValor(), v, motivo, u);
        actual.setValor(v);
        actual.setComentario(comentario);
        actual.setRegistradoPor(u.getId());
        actual.setFechaActualizacion(LocalDateTime.now());
        notaRepository.save(actual);
    }

    private void historial(Long evaluacionId, Long estudianteId, Double antes, Double despues, String motivo, Usuario u) {
        historialRepository.save(NotaHistorial.builder()
                .evaluacionId(evaluacionId).estudianteId(estudianteId)
                .valorAnterior(antes).valorNuevo(despues).motivo(motivo)
                .usuarioId(u == null ? null : u.getId()).usuarioNombre(u == null ? null : u.getNombre())
                .build());
    }

    private NotaDto.Boleta construirBoleta(Estudiante e, Integer anioPedido) {
        int anioActual = LocalDate.now().getYear();
        List<Nota> todas = notaRepository.findByEstudianteId(e.getId());
        TreeSet<Integer> anios = new TreeSet<>(Comparator.reverseOrder());
        anios.add(anioActual);
        todas.forEach(n -> anios.add(n.getEvaluacion().getPeriodo().getAnio()));
        int anio = anioPedido == null ? anioActual : anioPedido;

        List<Periodo> periodos = periodoRepository.findAll().stream()
                .filter(p -> p.getAnio() == anio)
                .sorted(Comparator.comparing(Periodo::getBimestre)).toList();

        // Cursos: los que tienen notas del alumno en el año + (año actual) los de su sección.
        LinkedHashMap<Long, SeccionCurso> cursos = new LinkedHashMap<>();
        Map<Long, Nota> notaPorEvaluacion = new HashMap<>();
        for (Nota n : todas) {
            if (n.getEvaluacion().getPeriodo().getAnio() != anio) continue;
            notaPorEvaluacion.put(n.getEvaluacion().getId(), n);
            cursos.putIfAbsent(n.getEvaluacion().getSeccionCurso().getId(), n.getEvaluacion().getSeccionCurso());
        }
        String grado = null, letra = null;
        if (anio == anioActual) {
            grado = e.getGrado();
            letra = e.getSeccion();
            accesoService.seccionDe(e).ifPresent(s -> seccionCursoRepository.findBySeccionId(s.getId()).stream()
                    .filter(sc -> "Activo".equals(sc.getEstado()))
                    .forEach(sc -> cursos.putIfAbsent(sc.getId(), sc)));
        } else if (!cursos.isEmpty()) {
            Seccion s = cursos.values().iterator().next().getSeccion();
            grado = s.getGrado();
            letra = s.getLetra();
        }

        List<NotaDto.CursoBoleta> filas = new ArrayList<>();
        Map<Long, List<Double>> porPeriodo = new HashMap<>();
        List<Double> anuales = new ArrayList<>();
        long aprobados = 0, desaprobados = 0;
        for (SeccionCurso sc : cursos.values()) {
            List<NotaDto.CursoPeriodo> cps = new ArrayList<>();
            List<Double> promsCurso = new ArrayList<>();
            for (Periodo p : periodos) {
                List<Evaluacion> evs = evaluacionRepository.findBySeccionCursoIdAndPeriodoIdOrderByFechaRegistroAsc(sc.getId(), p.getId());
                List<NotaDto.NotaDetalle> det = new ArrayList<>();
                List<Calificacion.Ponderada> pond = new ArrayList<>();
                CalculoParticipacion part = calcularParticipacion(evs, List.of(e));
                if (!part.actividades().isEmpty()) {
                    Double v = part.promedios().get(e.getId());
                    det.add(new NotaDto.NotaDetalle("Participación", "PARTICIPACION", 1, part.fechaRegistro(),
                            v, "Promedio de tus tareas, cuestionarios y actividades."));
                    if (v != null) pond.add(new Calificacion.Ponderada(v, 1));
                }
                for (Evaluacion ev : evs) {
                    if (ev.getActividadId() != null) continue;
                    Nota n = notaPorEvaluacion.get(ev.getId());
                    det.add(new NotaDto.NotaDetalle(ev.getNombre(), ev.getTipo(), ev.getPeso(), ev.getFecha(),
                            n == null ? null : n.getValor(), n == null ? null : n.getComentario()));
                    if (n != null) pond.add(new Calificacion.Ponderada(n.getValor(), ev.getPeso()));
                }
                Double prom = Calificacion.ponderado(pond);
                promsCurso.add(prom);
                if (prom != null) porPeriodo.computeIfAbsent(p.getId(), k -> new ArrayList<>()).add(prom);
                cps.add(new NotaDto.CursoPeriodo(p.getId(), Calificacion.promedio(prom), det));
            }
            Double anual = Calificacion.simple(promsCurso);
            if (anual != null) {
                anuales.add(anual);
                if (Calificacion.aprobado(anual)) aprobados++; else desaprobados++;
            }
            filas.add(new NotaDto.CursoBoleta(sc.getId(), sc.getCurso().getNombre(), sc.getCurso().getColor(),
                    sc.getCurso().getCategoria(), sc.getDocente() == null ? null : sc.getDocente().getNombreCompleto(),
                    cps, Calificacion.promedio(anual)));
        }
        filas.sort(Comparator.comparingInt((NotaDto.CursoBoleta c) -> CategoriaCurso.orden(c.categoria()))
                .thenComparing(NotaDto.CursoBoleta::curso, String.CASE_INSENSITIVE_ORDER));

        List<NotaDto.PromedioPeriodo> generales = periodos.stream()
                .map(p -> new NotaDto.PromedioPeriodo(p.getId(),
                        Calificacion.promedio(Calificacion.simple(porPeriodo.getOrDefault(p.getId(), List.of())))))
                .toList();
        return new NotaDto.Boleta(e.getId(), e.getNombreCompleto(), grado, letra, anio, new ArrayList<>(anios),
                periodos.stream().map(NotaService::corto).toList(), filas, generales,
                Calificacion.promedio(Calificacion.simple(anuales)), aprobados, desaprobados);
    }

    private NotaDto.Historial construirHistorial(Estudiante e) {
        int anioActual = LocalDate.now().getYear();
        TreeSet<Integer> anios = new TreeSet<>(Comparator.reverseOrder());
        notaRepository.findByEstudianteId(e.getId()).forEach(n -> anios.add(n.getEvaluacion().getPeriodo().getAnio()));
        if (e.getGrado() != null && !e.getGrado().isBlank()) anios.add(anioActual);
        List<Asistencia> asistencias = asistenciaRepository.findByEstudianteId(e.getId());

        List<NotaDto.AnioHistorial> lista = new ArrayList<>();
        for (int anio : anios) {
            NotaDto.Boleta b = construirBoleta(e, anio);
            boolean actual = anio == anioActual;
            Double asis = AsistenciaService.conteo(asistencias.stream()
                    .filter(a -> a.getFecha().getYear() == anio).toList()).porcentaje();
            List<Periodo> delAnio = periodoRepository.findAll().stream().filter(p -> p.getAnio() == anio).toList();
            boolean finalizado = !actual || (!delAnio.isEmpty()
                    && delAnio.stream().allMatch(p -> p.getFechaFin().isBefore(LocalDate.now())));
            String situacion;
            if (b.promedioAnual().valor() == null) situacion = actual ? "En curso" : "Sin calificaciones";
            else if (!finalizado) situacion = "En curso";
            else if (b.cursosDesaprobados() == 0) situacion = "Aprobado";
            else situacion = "Con " + b.cursosDesaprobados() + (b.cursosDesaprobados() == 1 ? " curso desaprobado" : " cursos desaprobados");
            lista.add(new NotaDto.AnioHistorial(anio, b.grado(), b.letra(), actual, b.promediosGenerales(), b.periodos(),
                    b.promedioAnual(), b.cursos().size(), b.cursosAprobados(), b.cursosDesaprobados(), asis, situacion));
        }
        return new NotaDto.Historial(e.getId(), e.getNombreCompleto(), e.getGrado(), e.getSeccion(), e.getAnioIngreso(), lista);
    }

    private SeccionCurso cursoParaLeer(Long usuarioId, Long seccionCursoId) {
        Usuario u = accesoService.usuario(usuarioId);
        SeccionCurso sc = accesoService.seccionCurso(seccionCursoId);
        if (AccesoService.esDocenteDe(u.getId(), sc) || AccesoService.esAdministrativo(u)) return sc;
        throw new AccesoDenegadoException("Este curso no está a tu cargo.");
    }

    private void aplicar(Evaluacion ev, NotaDto.EvaluacionRequest req) {
        String nombre = req.nombre() == null ? "" : req.nombre().trim().replaceAll("\\s+", " ");
        if (nombre.isEmpty()) throw new DatosInvalidosException("Escribe el nombre de la evaluación.");
        if (nombre.length() > 120) throw new DatosInvalidosException("El nombre admite hasta 120 caracteres.");
        String tipo = req.tipo() == null || req.tipo().isBlank() ? "OTRO" : req.tipo().trim().toUpperCase();
        if (!Calificacion.TIPOS.contains(tipo)) throw new DatosInvalidosException("Tipo de evaluación no válido.");
        int peso = req.peso() == null ? 1 : req.peso();
        if (peso < 1 || peso > 10) throw new DatosInvalidosException("El peso debe estar entre 1 y 10.");
        ev.setNombre(nombre);
        ev.setTipo(tipo);
        ev.setPeso(peso);
        ev.setFecha(req.fecha() == null ? LocalDate.now() : req.fecha());
    }

    private Evaluacion evaluacion(Long id) {
        if (id == null) throw new RecursoNoEncontradoException("Evaluación no encontrada.");
        return evaluacionRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Evaluación no encontrada."));
    }

    /** periodoId pedido; si no, el activo; si no hay activo, el más reciente. */
    public Periodo elegirPeriodo(Long periodoId) {
        if (periodoId != null) {
            return periodoRepository.findById(periodoId).orElseThrow(() -> new RecursoNoEncontradoException("Periodo no encontrado."));
        }
        Periodo activo = periodoActivo();
        if (activo != null) return activo;
        List<Periodo> todos = periodosOrdenados();
        return todos.isEmpty() ? null : todos.get(0);
    }

    /** Periodo activo = el que contiene la fecha de hoy; null entre bimestres o en vacaciones. */
    public Periodo periodoActivo() {
        LocalDate hoy = LocalDate.now();
        return periodoRepository.findAll().stream().filter(p -> p.vigenteEn(hoy)).findFirst().orElse(null);
    }

    /** Notas registradas de las evaluaciones indicadas (para los reportes). */
    @Transactional(readOnly = true)
    public List<Nota> notasDe(Collection<Long> evaluacionIds) {
        return evaluacionIds == null || evaluacionIds.isEmpty() ? List.of() : notaRepository.findByEvaluacionIdIn(evaluacionIds);
    }

    /** Del más reciente al más antiguo. */
    public List<Periodo> periodosOrdenados() {
        return periodoRepository.findAll().stream()
                .sorted(Comparator.comparing(Periodo::getAnio).reversed().thenComparing(Periodo::getBimestre, Comparator.reverseOrder()))
                .toList();
    }

    public static NotaDto.PeriodoCorto corto(Periodo p) {
        return new NotaDto.PeriodoCorto(p.getId(), p.getNombre(), p.getAnio(), p.getBimestre(), p.vigenteEn(LocalDate.now()));
    }

    // ------------------------------------------------------------------
    // Participación
    // ------------------------------------------------------------------

    /** actividades por id de evaluación; detalle y promedio por alumno. */
    private record CalculoParticipacion(Map<Long, Actividad> actividades,
                                        Map<Long, List<NotaDto.ParticipacionDetalle>> detalle,
                                        Map<Long, Double> promedios, LocalDate fechaRegistro) {}

    /**
     * Participación = promedio de las tareas, cuestionarios y actividades VISIBLES del periodo.
     * Calificadas: su nota. Vencidas sin entrega: 0. Entregadas sin calificar o aún en plazo: no cuentan todavía.
     */
    private CalculoParticipacion calcularParticipacion(List<Evaluacion> evs, Collection<Estudiante> alumnos) {
        Map<Long, Actividad> acts = new LinkedHashMap<>();
        evs.stream().filter(ev -> ev.getActividadId() != null).forEach(ev -> actividadRepository.findById(ev.getActividadId())
                .filter(a -> !Boolean.FALSE.equals(a.getVisible()))
                .ifPresent(a -> acts.put(ev.getId(), a)));
        if (acts.isEmpty()) return new CalculoParticipacion(acts, Map.of(), Map.of(), null);
        List<Map.Entry<Long, Actividad>> orden = new ArrayList<>(acts.entrySet());
        orden.sort(Comparator.comparing((Map.Entry<Long, Actividad> en) -> en.getValue().getFechaLimite()));
        Map<String, Double> notas = new HashMap<>();
        for (Nota n : notaRepository.findByEvaluacionIdIn(acts.keySet())) {
            notas.put(n.getEvaluacion().getId() + "-" + n.getEstudiante().getId(), n.getValor());
        }
        LocalDateTime ahora = LocalDateTime.now();
        Map<Long, List<NotaDto.ParticipacionDetalle>> detalle = new HashMap<>();
        Map<Long, Double> promedios = new HashMap<>();
        for (Estudiante e : alumnos) {
            List<NotaDto.ParticipacionDetalle> det = new ArrayList<>();
            List<Double> valores = new ArrayList<>();
            for (Map.Entry<Long, Actividad> en : orden) {
                Actividad a = en.getValue();
                Double v = notas.get(en.getKey() + "-" + e.getId());
                String estado;
                if (v != null) {
                    estado = "CALIFICADA";
                    valores.add(v);
                } else if (entregaRepository.findByActividadIdAndEstudianteId(a.getId(), e.getId()).isPresent()) {
                    estado = "POR_CALIFICAR";
                } else if (ahora.isAfter(a.getFechaLimite())) {
                    estado = "NO_ENTREGADA";
                    v = 0.0;
                    valores.add(0.0);
                } else {
                    estado = "PENDIENTE";
                }
                det.add(new NotaDto.ParticipacionDetalle(a.getId(), a.getTitulo(), a.getTipo(), v, estado,
                        a.getFechaLimite().toLocalDate()));
            }
            detalle.put(e.getId(), det);
            Double prom = Calificacion.simple(valores);
            promedios.put(e.getId(), prom == null ? null : Calificacion.dosDecimales(prom));
        }
        LocalDate primera = acts.values().stream().map(a -> a.getFechaPublicacion().toLocalDate())
                .min(Comparator.naturalOrder()).orElse(null);
        return new CalculoParticipacion(acts, detalle, promedios, primera);
    }

    static String foto(Estudiante e) {
        return e.getUsuario() == null ? null : e.getUsuario().getFoto();
    }

    static String recortar(String texto, int max) {
        if (texto == null) return null;
        String t = texto.trim();
        if (t.isEmpty()) return null;
        return t.length() > max ? t.substring(0, max) : t;
    }
}
