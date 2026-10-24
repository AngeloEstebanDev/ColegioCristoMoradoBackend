package com.CristoMorado_api.CristoMorado_api.services;

import com.CristoMorado_api.CristoMorado_api.dto.AsistenciaDto;
import com.CristoMorado_api.CristoMorado_api.dto.ReporteDto;
import com.CristoMorado_api.CristoMorado_api.entity.*;
import com.CristoMorado_api.CristoMorado_api.exception.DatosInvalidosException;
import com.CristoMorado_api.CristoMorado_api.exception.RecursoNoEncontradoException;
import com.CristoMorado_api.CristoMorado_api.repository.*;
import com.CristoMorado_api.CristoMorado_api.util.Calificacion;
import com.CristoMorado_api.CristoMorado_api.util.Grados;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * HU-31 Generación de reportes académicos (requiere el permiso REPORTES, ver PermisoInterceptor).
 * Cada reporte es una tabla que se muestra en pantalla y se exporta a Excel (HU-32).
 *  - rendimiento: por sección (alumnos x cursos) o resumen de todas las secciones.
 *  - cursos: promedio y aprobación de cada curso de cada sección.
 *  - asistencia: por sección (alumnos) o resumen de todas las secciones.
 *  - riesgo: alumnos con cursos desaprobados o asistencia menor al 80 %.
 *  - entregas: cumplimiento de tareas/cuestionarios por curso.
 */
@Service
public class ReporteService {

    public static final List<String> TIPOS = List.of("rendimiento", "cursos", "asistencia", "riesgo", "entregas",
            "notas", "notas-alumno", "asistencia-alumno");
    /** Máximo de filas por reporte (pantalla, Excel y PDF). Con más datos se pide usar los filtros. */
    public static final int LIMITE_FILAS = 20000;
    private static final Map<String, String> ESTADO_ASISTENCIA = Map.of(
            "PRESENTE", "Asistió", "TARDANZA", "Tardanza", "FALTA", "Falta", "JUSTIFICADA", "Justificada");
    private static final double ASISTENCIA_MINIMA = 80.0;
    private static final DateTimeFormatter DMY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final SeccionRepository seccionRepository;
    private final SeccionCursoRepository seccionCursoRepository;
    private final EvaluacionRepository evaluacionRepository;
    private final AsistenciaRepository asistenciaRepository;
    private final ActividadRepository actividadRepository;
    private final EntregaRepository entregaRepository;
    private final AccesoService accesoService;
    private final NotaService notaService;

    public ReporteService(SeccionRepository seccionRepository, SeccionCursoRepository seccionCursoRepository,
                          EvaluacionRepository evaluacionRepository, AsistenciaRepository asistenciaRepository,
                          ActividadRepository actividadRepository, EntregaRepository entregaRepository,
                          AccesoService accesoService, NotaService notaService) {
        this.seccionRepository = seccionRepository;
        this.seccionCursoRepository = seccionCursoRepository;
        this.evaluacionRepository = evaluacionRepository;
        this.asistenciaRepository = asistenciaRepository;
        this.actividadRepository = actividadRepository;
        this.entregaRepository = entregaRepository;
        this.accesoService = accesoService;
        this.notaService = notaService;
    }

    @Transactional(readOnly = true)
    public ReporteDto.Filtros filtros() {
        List<ReporteDto.Opcion> periodos = notaService.periodosOrdenados().stream()
                .map(p -> new ReporteDto.Opcion(p.getId(), p.getNombre())).toList();
        Periodo activo = notaService.periodoActivo();
        List<ReporteDto.Opcion> secciones = seccionesActivas().stream()
                .map(s -> new ReporteDto.Opcion(s.getId(), AccesoService.salon(s))).toList();
        return new ReporteDto.Filtros(periodos, activo == null ? null : activo.getId(), secciones);
    }

    /** Opciones de los filtros: periodos, grados, secciones, cursos y alumnos matriculados. */
    @Transactional(readOnly = true)
    public ReporteDto.Opciones opciones() {
        List<ReporteDto.Opcion> periodos = notaService.periodosOrdenados().stream()
                .map(p -> new ReporteDto.Opcion(p.getId(), p.getNombre())).toList();
        Periodo activo = notaService.periodoActivo();
        List<Seccion> secciones = seccionesActivas();
        List<String> grados = secciones.stream().map(Seccion::getGrado).distinct().toList();
        Map<Long, String> cursos = new TreeMap<>();
        List<ReporteDto.AlumnoOpcion> alumnos = new ArrayList<>();
        for (Seccion s : secciones) {
            for (SeccionCurso sc : cursosActivos(s)) cursos.putIfAbsent(sc.getCurso().getId(), sc.getCurso().getNombre());
            for (Estudiante e : accesoService.alumnosDe(s)) {
                alumnos.add(new ReporteDto.AlumnoOpcion(e.getId(), e.getNombreCompleto(), s.getGrado(), s.getLetra(), s.getId()));
            }
        }
        List<ReporteDto.Opcion> listaCursos = cursos.entrySet().stream()
                .map(e -> new ReporteDto.Opcion(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(ReporteDto.Opcion::nombre, String.CASE_INSENSITIVE_ORDER)).toList();
        return new ReporteDto.Opciones(periodos, activo == null ? null : activo.getId(), grados,
                secciones.stream().map(s -> new ReporteDto.SeccionOpcion(s.getId(), s.getGrado(), s.getLetra(), AccesoService.salon(s))).toList(),
                listaCursos, alumnos);
    }

    @Transactional(readOnly = true)
    public ReporteDto.Tabla generar(String tipo, Long periodoId, Long seccionId, LocalDate desde, LocalDate hasta) {
        return generar(tipo, periodoId, seccionId, desde, hasta, null, null, null);
    }

    @Transactional(readOnly = true)
    public ReporteDto.Tabla generar(String tipo, Long periodoId, Long seccionId, LocalDate desde, LocalDate hasta,
                                    String grado, Long estudianteId, Long cursoId) {
        String t = tipo == null ? "" : tipo.trim().toLowerCase();
        ReporteDto.Filtro f = new ReporteDto.Filtro(periodoId, grado == null || grado.isBlank() ? null : grado.trim(),
                seccionId, estudianteId, cursoId);
        return switch (t) {
            case "notas" -> notasSeccion(f);
            case "notas-alumno" -> notasPorAlumno(f);
            case "asistencia-alumno" -> asistenciaPorAlumno(f);
            case "rendimiento" -> seccionId == null ? rendimientoGeneral(periodo(periodoId)) : rendimientoSeccion(periodo(periodoId), seccion(seccionId));
            case "cursos" -> cursos(periodo(periodoId));
            case "asistencia" -> asistencia(periodoId, seccionId, desde, hasta);
            case "riesgo" -> riesgo(periodo(periodoId));
            case "entregas" -> entregas(periodo(periodoId));
            default -> throw new DatosInvalidosException("Tipo de reporte no válido: " + tipo);
        };
    }

    // ------------------------------------------------------------------
    // Centro de Control de Reportes: notas y asistencia por alumno
    // ------------------------------------------------------------------

    /**
     * Exportar notas: promedios por curso de los alumnos. Grado, sección y periodo pueden ser
     * "Todos" (null): con una sola sección y un periodo se devuelve el registro clásico; si no,
     * se unen los registros de cada sección y periodo con columnas Grado, Sección y Período al inicio
     * (los cursos que una sección no lleva quedan en blanco).
     */
    private ReporteDto.Tabla notasSeccion(ReporteDto.Filtro f) {
        List<Seccion> secciones = seccionesFiltradas(f);
        List<Periodo> periodos = periodosFiltrados(f);
        if (secciones.isEmpty()) throw new DatosInvalidosException("No hay secciones activas con los filtros elegidos.");
        if (periodos.isEmpty()) throw new DatosInvalidosException("No hay periodos académicos registrados.");

        if (secciones.size() == 1 && periodos.size() == 1) {
            Seccion s = secciones.get(0);
            ReporteDto.Tabla t = rendimientoSeccion(periodos.get(0), s);
            return new ReporteDto.Tabla("notas", "Registro de notas - " + AccesoService.salon(s), t.subtitulo(), t.columnas(), t.filas(), t.indicadores());
        }

        List<ReporteDto.Tabla> partes = new ArrayList<>();
        List<Seccion> secPartes = new ArrayList<>();
        List<Periodo> perPartes = new ArrayList<>();
        java.util.LinkedHashSet<String> cursos = new java.util.LinkedHashSet<>();
        for (Seccion s : secciones) {
            for (Periodo p : periodos) {
                ReporteDto.Tabla t = rendimientoSeccion(p, s);
                partes.add(t); secPartes.add(s); perPartes.add(p);
                cursos.addAll(t.columnas().subList(2, t.columnas().size() - 4));
            }
        }
        List<String> columnas = new ArrayList<>(List.of("Grado", "Sección", "Período", "N°", "Alumno"));
        columnas.addAll(cursos);
        columnas.addAll(List.of("Promedio", "Literal", "Puesto", "Situación"));

        List<List<Object>> filas = new ArrayList<>();
        int total = 0;
        java.util.Set<String> alumnos = new java.util.HashSet<>();
        List<Double> promedios = new ArrayList<>();
        long aprobados = 0, conDesaprobados = 0;
        for (int i = 0; i < partes.size(); i++) {
            ReporteDto.Tabla t = partes.get(i);
            Seccion s = secPartes.get(i);
            List<String> cols = t.columnas();
            int nCursos = cols.size() - 6;
            for (List<Object> fila : t.filas()) {
                total++;
                alumnos.add(s.getId() + "|" + fila.get(1));
                Object prom = fila.get(2 + nCursos);
                Object situacion = fila.get(5 + nCursos);
                if (prom instanceof Number num) promedios.add(num.doubleValue());
                if ("Aprobado".equals(situacion)) aprobados++;
                else if (situacion != null && String.valueOf(situacion).startsWith("Desaprobado")) conDesaprobados++;
                if (filas.size() >= LIMITE_FILAS) continue;
                List<Object> nueva = new ArrayList<>(List.of(s.getGrado(), s.getLetra(), nombrePeriodo(perPartes.get(i)), fila.get(0), fila.get(1)));
                for (String c : cursos) {
                    int k = cols.indexOf(c);
                    nueva.add(k >= 2 && k < 2 + nCursos ? fila.get(k) : "");
                }
                nueva.addAll(fila.subList(2 + nCursos, fila.size()));
                filas.add(nueva);
            }
        }
        Double promGeneral = Calificacion.simple(promedios);
        List<ReporteDto.Indicador> ind = new ArrayList<>(List.of(
                new ReporteDto.Indicador("Secciones", String.valueOf(secciones.size()), null),
                new ReporteDto.Indicador("Periodos", String.valueOf(periodos.size()), null),
                new ReporteDto.Indicador("Alumnos", String.valueOf(alumnos.size()), null),
                new ReporteDto.Indicador("Promedio general", texto(promGeneral), Calificacion.literal(promGeneral)),
                new ReporteDto.Indicador("Aprobados en todo", String.valueOf(aprobados), "Registros alumno-periodo"),
                new ReporteDto.Indicador("Con cursos desaprobados", String.valueOf(conDesaprobados), "Registros alumno-periodo")));
        if (total > LIMITE_FILAS) ind.add(limite(total));
        String titulo = f.seccionId() != null ? "Registro de notas - " + AccesoService.salon(secciones.get(0))
                : f.grado() != null ? "Registro de notas - " + f.grado() : "Registro de notas - Todos los grados";
        return new ReporteDto.Tabla("notas", titulo,
                descripcion(f, secciones, periodos) + " · Notas vigesimales (0 a 20) · Aprueba con " + Calificacion.APROBATORIA,
                columnas, filas, ind);
    }

    /** Reporte por alumno: cada nota registrada (grado, sección, alumno, periodo, curso, evaluación y nota). */
    private ReporteDto.Tabla notasPorAlumno(ReporteDto.Filtro f) {
        List<Seccion> secciones = seccionesFiltradas(f);
        List<Periodo> periodos = periodosFiltrados(f);
        Set<Long> idsPeriodo = new HashSet<>();
        periodos.forEach(p -> idsPeriodo.add(p.getId()));

        List<Object[]> registros = new ArrayList<>();   // {Seccion, Estudiante, Evaluacion, Nota}
        for (Seccion s : secciones) {
            Map<Long, SeccionCurso> cursos = new HashMap<>();
            for (SeccionCurso sc : cursosActivos(s)) {
                if (f.cursoId() == null || sc.getCurso().getId().equals(f.cursoId())) cursos.put(sc.getId(), sc);
            }
            if (cursos.isEmpty()) continue;
            Map<Long, Evaluacion> evs = new HashMap<>();
            for (Evaluacion ev : evaluacionRepository.findBySeccionCursoIdIn(cursos.keySet())) {
                if (ev.getPeriodo() != null && idsPeriodo.contains(ev.getPeriodo().getId())
                        && ev.getSeccionCurso() != null && cursos.containsKey(ev.getSeccionCurso().getId())) evs.put(ev.getId(), ev);
            }
            for (Nota n : notaService.notasDe(evs.keySet())) {
                Estudiante e = n.getEstudiante();
                if (e == null || (f.estudianteId() != null && !e.getId().equals(f.estudianteId()))) continue;
                registros.add(new Object[]{s, e, evs.get(n.getEvaluacion().getId()), n});
            }
        }
        registros.sort(Comparator
                .comparingInt((Object[] r) -> Grados.orden(((Seccion) r[0]).getGrado()))
                .thenComparing(r -> ((Seccion) r[0]).getLetra())
                .thenComparing(r -> ((Estudiante) r[1]).getNombreCompleto(), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(r -> ((Evaluacion) r[2]).getPeriodo().getBimestre())
                .thenComparing(r -> ((Evaluacion) r[2]).getSeccionCurso().getCurso().getNombre(), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(r -> ((Evaluacion) r[2]).getFecha() == null ? LocalDate.MAX : ((Evaluacion) r[2]).getFecha())
                .thenComparing(r -> ((Evaluacion) r[2]).getId()));

        List<List<Object>> filas = new ArrayList<>();
        Set<Long> alumnos = new HashSet<>();
        List<Double> valores = new ArrayList<>();
        long desaprobadas = 0;
        for (Object[] r : registros) {
            Seccion s = (Seccion) r[0];
            Estudiante e = (Estudiante) r[1];
            Evaluacion ev = (Evaluacion) r[2];
            Nota n = (Nota) r[3];
            alumnos.add(e.getId());
            if (n.getValor() != null) {
                valores.add(n.getValor());
                if (!Calificacion.aprobado(n.getValor())) desaprobadas++;
            }
            if (filas.size() < LIMITE_FILAS) {
                filas.add(new ArrayList<>(List.of(s.getGrado(), s.getLetra(), e.getNombreCompleto(), nombrePeriodo(ev.getPeriodo()),
                        ev.getSeccionCurso().getCurso().getNombre(), ev.getNombre(), celda(n.getValor() == null ? null : Calificacion.dosDecimales(n.getValor())))));
            }
        }
        Double promedio = Calificacion.simple(valores);
        List<ReporteDto.Indicador> ind = new ArrayList<>(List.of(
                new ReporteDto.Indicador("Alumnos", String.valueOf(alumnos.size()), null),
                new ReporteDto.Indicador("Notas registradas", String.valueOf(registros.size()), null),
                new ReporteDto.Indicador("Promedio", texto(promedio), Calificacion.literal(promedio)),
                new ReporteDto.Indicador("Notas desaprobadas", String.valueOf(desaprobadas), null)));
        if (registros.size() > LIMITE_FILAS) ind.add(limite(registros.size()));
        return new ReporteDto.Tabla("notas-alumno", "Reporte de notas por alumno", descripcion(f, secciones, periodos),
                List.of("Grado", "Sección", "Alumno", "Período", "Curso", "Actividad / Evaluación", "Nota"), filas, ind);
    }

    /** Asistencia del alumno: cada registro de asistencia (grado, sección, alumno, periodo, curso, fecha y estado). */
    private ReporteDto.Tabla asistenciaPorAlumno(ReporteDto.Filtro f) {
        List<Seccion> secciones = seccionesFiltradas(f);
        List<Periodo> periodos = periodosFiltrados(f);
        if (periodos.isEmpty()) throw new DatosInvalidosException("Aún no hay periodos registrados. Créalos en Académico > Periodos.");
        LocalDate desde = periodos.stream().map(Periodo::getFechaInicio).min(Comparator.naturalOrder()).orElseThrow();
        LocalDate hasta = periodos.stream().map(Periodo::getFechaFin).max(Comparator.naturalOrder()).orElseThrow();

        List<Asistencia> registros = new ArrayList<>();
        Map<Long, Seccion> seccionDe = new HashMap<>();
        for (Seccion s : secciones) {
            for (SeccionCurso sc : cursosActivos(s)) {
                if (f.cursoId() != null && !sc.getCurso().getId().equals(f.cursoId())) continue;
                for (Asistencia a : asistenciaRepository.findBySeccionCursoIdAndFechaBetween(sc.getId(), desde, hasta)) {
                    if (a.getEstudiante() == null || (f.estudianteId() != null && !a.getEstudiante().getId().equals(f.estudianteId()))) continue;
                    if (periodoDe(periodos, a.getFecha()) == null) continue;
                    registros.add(a);
                    seccionDe.put(a.getId(), s);
                }
            }
        }
        registros.sort(Comparator
                .comparingInt((Asistencia a) -> Grados.orden(seccionDe.get(a.getId()).getGrado()))
                .thenComparing(a -> seccionDe.get(a.getId()).getLetra())
                .thenComparing(a -> a.getEstudiante().getNombreCompleto(), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(a -> a.getSeccionCurso().getCurso().getNombre(), String.CASE_INSENSITIVE_ORDER)
                .thenComparing(Asistencia::getFecha));

        List<List<Object>> filas = new ArrayList<>();
        Set<Long> alumnos = new HashSet<>();
        Map<String, Long> conteo = new HashMap<>();
        for (Asistencia a : registros) {
            Seccion s = seccionDe.get(a.getId());
            alumnos.add(a.getEstudiante().getId());
            conteo.merge(a.getEstado(), 1L, Long::sum);
            if (filas.size() < LIMITE_FILAS) {
                filas.add(new ArrayList<>(List.of(s.getGrado(), s.getLetra(), a.getEstudiante().getNombreCompleto(),
                        nombrePeriodo(periodoDe(periodos, a.getFecha())), a.getSeccionCurso().getCurso().getNombre(),
                        a.getFecha().format(DMY), ESTADO_ASISTENCIA.getOrDefault(a.getEstado(), a.getEstado()))));
            }
        }
        long presentes = conteo.getOrDefault("PRESENTE", 0L), tardanzas = conteo.getOrDefault("TARDANZA", 0L);
        long faltas = conteo.getOrDefault("FALTA", 0L), justificadas = conteo.getOrDefault("JUSTIFICADA", 0L);
        List<ReporteDto.Indicador> ind = new ArrayList<>(List.of(
                new ReporteDto.Indicador("Alumnos", String.valueOf(alumnos.size()), null),
                new ReporteDto.Indicador("Registros", String.valueOf(registros.size()), null),
                new ReporteDto.Indicador("Asistió", String.valueOf(presentes), null),
                new ReporteDto.Indicador("Tardanzas", String.valueOf(tardanzas), null),
                new ReporteDto.Indicador("Faltas", String.valueOf(faltas), null),
                new ReporteDto.Indicador("Justificadas", String.valueOf(justificadas), null),
                new ReporteDto.Indicador("% asistencia", textoPct(porcentaje(presentes + tardanzas + justificadas, registros.size())), null)));
        if (registros.size() > LIMITE_FILAS) ind.add(limite(registros.size()));
        return new ReporteDto.Tabla("asistencia-alumno", "Reporte de asistencia por alumno", descripcion(f, secciones, periodos),
                List.of("Grado", "Sección", "Alumno", "Período", "Curso", "Fecha", "Estado de asistencia"), filas, ind);
    }

    /** Secciones activas según el grado y la sección elegidos (null = todas). */
    private List<Seccion> seccionesFiltradas(ReporteDto.Filtro f) {
        if (f.grado() != null && !Grados.valido(f.grado())) throw new DatosInvalidosException("Grado no válido: " + f.grado());
        if (f.seccionId() != null) {
            Seccion s = seccion(f.seccionId());
            if (f.grado() != null && !s.getGrado().equals(f.grado())) {
                throw new DatosInvalidosException("La sección elegida no pertenece al grado " + f.grado() + ".");
            }
            return List.of(s);
        }
        return seccionesActivas().stream().filter(s -> f.grado() == null || s.getGrado().equals(f.grado())).toList();
    }

    /** Periodo elegido o, si es "Todos", los del año más reciente. */
    private List<Periodo> periodosFiltrados(ReporteDto.Filtro f) {
        if (f.periodoId() != null) return List.of(periodo(f.periodoId()));
        List<Periodo> todos = notaService.periodosOrdenados();
        if (todos.isEmpty()) return List.of();
        int anio = todos.get(0).getAnio();
        return todos.stream().filter(p -> p.getAnio() == anio).sorted(Comparator.comparing(Periodo::getBimestre)).toList();
    }

    private static Periodo periodoDe(List<Periodo> periodos, LocalDate fecha) {
        for (Periodo p : periodos) {
            if (!fecha.isBefore(p.getFechaInicio()) && !fecha.isAfter(p.getFechaFin())) return p;
        }
        return null;
    }

    /** "I Bimestre 2026" -> "I Bimestre". */
    private static String nombrePeriodo(Periodo p) {
        if (p == null) return "—";
        String n = p.getNombre() == null ? p.getBimestre() + "° Bimestre" : p.getNombre();
        return n.replaceAll("\\s+\\d{4}$", "");
    }

    private String descripcion(ReporteDto.Filtro f, List<Seccion> secciones, List<Periodo> periodos) {
        String salon = f.seccionId() != null && !secciones.isEmpty() ? AccesoService.salon(secciones.get(0))
                : f.grado() != null ? f.grado() + " (todas las secciones)" : "Todos los grados";
        String per = f.periodoId() != null && !periodos.isEmpty() ? periodos.get(0).getNombre()
                : periodos.isEmpty() ? "Sin periodos" : "Todos los periodos " + periodos.get(0).getAnio();
        return salon + " · " + per + " · Generado el " + hoy();
    }

    private static ReporteDto.Indicador limite(int total) {
        return new ReporteDto.Indicador("Filas mostradas", LIMITE_FILAS + " de " + total,
                "Hay más registros: usa los filtros para acotar el reporte.");
    }

    // ------------------------------------------------------------------
    // Rendimiento
    // ------------------------------------------------------------------

    private ReporteDto.Tabla rendimientoSeccion(Periodo p, Seccion s) {
        List<SeccionCurso> cursos = cursosActivos(s);
        List<Estudiante> alumnos = accesoService.alumnosDe(s);
        Map<Long, Map<Long, Double>> proms = new HashMap<>();   // scId -> (alumno -> promedio)
        for (SeccionCurso sc : cursos) proms.put(sc.getId(), notaService.promediosDelCurso(sc, p));

        List<String> columnas = new ArrayList<>(List.of("N°", "Alumno"));
        cursos.forEach(sc -> columnas.add(sc.getCurso().getNombre()));
        columnas.addAll(List.of("Promedio", "Literal", "Puesto", "Situación"));

        Map<Long, Double> generales = new HashMap<>();
        Map<Long, Integer> desaprobados = new HashMap<>();
        for (Estudiante e : alumnos) {
            List<Double> valores = new ArrayList<>();
            int des = 0;
            for (SeccionCurso sc : cursos) {
                Double v = proms.get(sc.getId()).get(e.getId());
                valores.add(v);
                if (v != null && !Calificacion.aprobado(v)) des++;
            }
            generales.put(e.getId(), Calificacion.simple(valores));
            desaprobados.put(e.getId(), des);
        }
        Map<Long, Integer> puestos = puestos(generales);

        List<List<Object>> filas = new ArrayList<>();
        int n = 1;
        long aprobados = 0, conDesaprobados = 0;
        for (Estudiante e : alumnos) {
            List<Object> fila = new ArrayList<>(List.of(n++, e.getNombreCompleto()));
            for (SeccionCurso sc : cursos) fila.add(celda(Calificacion.redondear(proms.get(sc.getId()).get(e.getId()))));
            Double g = generales.get(e.getId());
            int des = desaprobados.get(e.getId());
            String situacion = g == null ? "Sin notas" : des == 0 ? "Aprobado" : "Desaprobado en " + des + (des == 1 ? " curso" : " cursos");
            if (g != null) { if (des == 0) aprobados++; else conDesaprobados++; }
            fila.add(celda(g));
            fila.add(celda(Calificacion.literal(g)));
            fila.add(celda(puestos.get(e.getId())));
            fila.add(situacion);
            filas.add(fila);
        }
        Double promSeccion = Calificacion.simple(generales.values());
        return new ReporteDto.Tabla("rendimiento", "Rendimiento académico - " + AccesoService.salon(s),
                p.getNombre() + " · Notas vigesimales (0 a 20) · Aprueba con " + Calificacion.APROBATORIA + " · Generado el " + hoy(),
                columnas, filas, List.of(
                        new ReporteDto.Indicador("Alumnos", String.valueOf(alumnos.size()), null),
                        new ReporteDto.Indicador("Promedio de la sección", texto(promSeccion), Calificacion.literal(promSeccion)),
                        new ReporteDto.Indicador("Aprobados en todo", String.valueOf(aprobados), null),
                        new ReporteDto.Indicador("Con cursos desaprobados", String.valueOf(conDesaprobados), null)));
    }

    private ReporteDto.Tabla rendimientoGeneral(Periodo p) {
        List<String> columnas = List.of("Grado", "Sección", "Turno", "Tutor", "Alumnos", "Cursos", "Promedio",
                "Literal", "Aprobados", "Con desaprobados", "Sin notas", "% Aprobación");
        List<List<Object>> filas = new ArrayList<>();
        List<Double> promedios = new ArrayList<>();
        long totalAlumnos = 0, totalAprob = 0, totalDes = 0;
        for (Seccion s : seccionesActivas()) {
            List<SeccionCurso> cursos = cursosActivos(s);
            List<Estudiante> alumnos = accesoService.alumnosDe(s);
            List<Map<Long, Double>> proms = cursos.stream().map(sc -> notaService.promediosDelCurso(sc, p)).toList();
            long aprob = 0, des = 0, sin = 0;
            List<Double> generales = new ArrayList<>();
            for (Estudiante e : alumnos) {
                List<Double> v = proms.stream().map(m -> m.get(e.getId())).toList();
                Double g = Calificacion.simple(v);
                if (g == null) { sin++; continue; }
                generales.add(g);
                if (v.stream().anyMatch(x -> x != null && !Calificacion.aprobado(x))) des++; else aprob++;
            }
            Double prom = Calificacion.simple(generales);
            if (prom != null) promedios.add(prom);
            totalAlumnos += alumnos.size();
            totalAprob += aprob;
            totalDes += des;
            filas.add(new ArrayList<>(List.of(s.getGrado(), s.getLetra(), s.getTurno(),
                    s.getTutor() == null ? "Sin tutor" : s.getTutor().getNombreCompleto(), alumnos.size(), cursos.size(),
                    celda(prom), celda(Calificacion.literal(prom)), aprob, des, sin, celda(porcentaje(aprob, aprob + des)))));
        }
        Double general = Calificacion.simple(promedios);
        return new ReporteDto.Tabla("rendimiento", "Rendimiento académico por sección",
                p.getNombre() + " · Generado el " + hoy(), columnas, filas, List.of(
                        new ReporteDto.Indicador("Secciones", String.valueOf(filas.size()), null),
                        new ReporteDto.Indicador("Alumnos", String.valueOf(totalAlumnos), null),
                        new ReporteDto.Indicador("Promedio del colegio", texto(general), Calificacion.literal(general)),
                        new ReporteDto.Indicador("% Aprobación", textoPct(porcentaje(totalAprob, totalAprob + totalDes)), null)));
    }

    // ------------------------------------------------------------------
    // Cursos
    // ------------------------------------------------------------------

    private ReporteDto.Tabla cursos(Periodo p) {
        List<String> columnas = List.of("Grado", "Sección", "Curso", "Docente", "Evaluaciones", "Alumnos con nota",
                "Promedio", "Literal", "Aprobados", "Desaprobados", "% Aprobación");
        List<List<Object>> filas = new ArrayList<>();
        long sinRegistro = 0;
        List<Double> promedios = new ArrayList<>();
        for (Seccion s : seccionesActivas()) {
            for (SeccionCurso sc : cursosActivos(s)) {
                int evals = evaluacionRepository.findBySeccionCursoIdAndPeriodoIdOrderByFechaRegistroAsc(sc.getId(), p.getId()).size();
                Collection<Double> proms = notaService.promediosDelCurso(sc, p).values();
                long aprob = proms.stream().filter(Calificacion::aprobado).count();
                long des = proms.size() - aprob;
                Double prom = Calificacion.simple(proms);
                if (prom != null) promedios.add(prom);
                if (evals == 0) sinRegistro++;
                filas.add(new ArrayList<>(List.of(s.getGrado(), s.getLetra(), sc.getCurso().getNombre(),
                        sc.getDocente() == null ? "Sin docente" : sc.getDocente().getNombreCompleto(), evals, proms.size(),
                        celda(prom), celda(Calificacion.literal(prom)), aprob, des, celda(porcentaje(aprob, aprob + des)))));
            }
        }
        return new ReporteDto.Tabla("cursos", "Resultados por curso", p.getNombre() + " · Generado el " + hoy(),
                columnas, filas, List.of(
                        new ReporteDto.Indicador("Cursos", String.valueOf(filas.size()), null),
                        new ReporteDto.Indicador("Sin evaluaciones registradas", String.valueOf(sinRegistro), null),
                        new ReporteDto.Indicador("Promedio general", texto(Calificacion.simple(promedios)), null)));
    }

    // ------------------------------------------------------------------
    // Asistencia
    // ------------------------------------------------------------------

    private ReporteDto.Tabla asistencia(Long periodoId, Long seccionId, LocalDate desde, LocalDate hasta) {
        LocalDate d = desde, h = hasta;
        if (d == null || h == null) {
            Periodo p = periodoId == null ? notaService.periodoActivo() : periodo(periodoId);
            if (d == null) d = p == null ? LocalDate.now().withDayOfYear(1) : p.getFechaInicio();
            if (h == null) h = p == null ? LocalDate.now() : (p.getFechaFin().isAfter(LocalDate.now()) ? LocalDate.now() : p.getFechaFin());
        }
        if (h.isBefore(d)) throw new DatosInvalidosException("La fecha final debe ser posterior a la inicial.");
        String rango = "Del " + d.format(DMY) + " al " + h.format(DMY);

        if (seccionId != null) {
            Seccion s = seccion(seccionId);
            Map<Long, List<Asistencia>> porAlumno = new HashMap<>();
            Set<LocalDate> dias = new HashSet<>();
            for (SeccionCurso sc : cursosActivos(s)) {
                for (Asistencia a : asistenciaRepository.findBySeccionCursoIdAndFechaBetween(sc.getId(), d, h)) {
                    porAlumno.computeIfAbsent(a.getEstudiante().getId(), k -> new ArrayList<>()).add(a);
                    dias.add(a.getFecha());
                }
            }
            List<List<Object>> filas = new ArrayList<>();
            int n = 1;
            long enRiesgo = 0;
            List<Double> pcts = new ArrayList<>();
            for (Estudiante e : accesoService.alumnosDe(s)) {
                AsistenciaDto.Conteo c = AsistenciaService.conteo(porAlumno.getOrDefault(e.getId(), List.of()));
                if (c.porcentaje() != null) {
                    pcts.add(c.porcentaje());
                    if (c.porcentaje() < ASISTENCIA_MINIMA) enRiesgo++;
                }
                filas.add(new ArrayList<>(List.of(n++, e.getNombreCompleto(), c.presentes(), c.tardanzas(), c.faltas(),
                        c.justificadas(), c.total(), celda(c.porcentaje()))));
            }
            return new ReporteDto.Tabla("asistencia", "Asistencia - " + AccesoService.salon(s), rango + " · Generado el " + hoy(),
                    List.of("N°", "Alumno", "Presentes", "Tardanzas", "Faltas", "Justificadas", "Registros", "% Asistencia"),
                    filas, List.of(
                            new ReporteDto.Indicador("Días con registro", String.valueOf(dias.size()), null),
                            new ReporteDto.Indicador("Asistencia promedio", textoPct(Calificacion.simple(pcts)), null),
                            new ReporteDto.Indicador("Alumnos bajo 80 %", String.valueOf(enRiesgo), null)));
        }

        List<List<Object>> filas = new ArrayList<>();
        List<Asistencia> todas = new ArrayList<>();
        for (Seccion s : seccionesActivas()) {
            List<Asistencia> lista = new ArrayList<>();
            for (SeccionCurso sc : cursosActivos(s)) lista.addAll(asistenciaRepository.findBySeccionCursoIdAndFechaBetween(sc.getId(), d, h));
            todas.addAll(lista);
            AsistenciaDto.Conteo c = AsistenciaService.conteo(lista);
            long dias = lista.stream().map(Asistencia::getFecha).distinct().count();
            filas.add(new ArrayList<>(List.of(s.getGrado(), s.getLetra(), accesoService.alumnosDe(s).size(), dias,
                    c.presentes(), c.tardanzas(), c.faltas(), c.justificadas(), celda(c.porcentaje()))));
        }
        AsistenciaDto.Conteo total = AsistenciaService.conteo(todas);
        return new ReporteDto.Tabla("asistencia", "Asistencia por sección", rango + " · Generado el " + hoy(),
                List.of("Grado", "Sección", "Alumnos", "Días con registro", "Presentes", "Tardanzas", "Faltas",
                        "Justificadas", "% Asistencia"),
                filas, List.of(
                        new ReporteDto.Indicador("Registros", String.valueOf(total.total()), null),
                        new ReporteDto.Indicador("Asistencia del colegio", textoPct(total.porcentaje()), null),
                        new ReporteDto.Indicador("Faltas", String.valueOf(total.faltas()), null)));
    }

    // ------------------------------------------------------------------
    // Alumnos en riesgo
    // ------------------------------------------------------------------

    private ReporteDto.Tabla riesgo(Periodo p) {
        LocalDate hasta = p.getFechaFin().isAfter(LocalDate.now()) ? LocalDate.now() : p.getFechaFin();
        List<List<Object>> filas = new ArrayList<>();
        long porNotas = 0, porAsistencia = 0;
        for (Seccion s : seccionesActivas()) {
            List<SeccionCurso> cursos = cursosActivos(s);
            Map<Long, Map<Long, Double>> proms = new HashMap<>();
            Map<Long, List<Asistencia>> asis = new HashMap<>();
            for (SeccionCurso sc : cursos) {
                proms.put(sc.getId(), notaService.promediosDelCurso(sc, p));
                for (Asistencia a : asistenciaRepository.findBySeccionCursoIdAndFechaBetween(sc.getId(), p.getFechaInicio(), hasta)) {
                    asis.computeIfAbsent(a.getEstudiante().getId(), k -> new ArrayList<>()).add(a);
                }
            }
            for (Estudiante e : accesoService.alumnosDe(s)) {
                List<String> desaprobados = new ArrayList<>();
                List<Double> valores = new ArrayList<>();
                for (SeccionCurso sc : cursos) {
                    Double v = proms.get(sc.getId()).get(e.getId());
                    valores.add(v);
                    if (v != null && !Calificacion.aprobado(v)) {
                        desaprobados.add(sc.getCurso().getNombre() + " (" + Calificacion.redondear(v) + ")");
                    }
                }
                Double pct = AsistenciaService.conteo(asis.getOrDefault(e.getId(), List.of())).porcentaje();
                boolean bajaAsistencia = pct != null && pct < ASISTENCIA_MINIMA;
                if (desaprobados.isEmpty() && !bajaAsistencia) continue;
                List<String> motivos = new ArrayList<>();
                if (!desaprobados.isEmpty()) { motivos.add("Notas"); porNotas++; }
                if (bajaAsistencia) { motivos.add("Asistencia"); porAsistencia++; }
                Double g = Calificacion.simple(valores);
                filas.add(new ArrayList<>(List.of(e.getNombreCompleto(), AccesoService.salon(s), desaprobados.size(),
                        desaprobados.isEmpty() ? "—" : String.join(", ", desaprobados), celda(g), celda(pct),
                        String.join(" y ", motivos))));
            }
        }
        filas.sort(Comparator.comparing((List<Object> f) -> -((Number) f.get(2)).intValue())
                .thenComparing(f -> String.valueOf(f.get(0))));
        return new ReporteDto.Tabla("riesgo", "Alumnos en riesgo académico",
                p.getNombre() + " · Curso desaprobado (menos de " + Calificacion.APROBATORIA + ") o asistencia menor al 80 % · Generado el " + hoy(),
                List.of("Alumno", "Grado y sección", "N° cursos desaprobados", "Cursos desaprobados", "Promedio general",
                        "% Asistencia", "Motivo"),
                filas, List.of(
                        new ReporteDto.Indicador("Alumnos en riesgo", String.valueOf(filas.size()), null),
                        new ReporteDto.Indicador("Por notas", String.valueOf(porNotas), null),
                        new ReporteDto.Indicador("Por asistencia", String.valueOf(porAsistencia), null)));
    }

    // ------------------------------------------------------------------
    // Cumplimiento de entregas
    // ------------------------------------------------------------------

    private ReporteDto.Tabla entregas(Periodo p) {
        LocalDateTime ini = p.getFechaInicio().atStartOfDay(), fin = p.getFechaFin().plusDays(1).atStartOfDay();
        List<List<Object>> filas = new ArrayList<>();
        long totalEsperadas = 0, totalEntregadas = 0, totalPorCalificar = 0, totalActividades = 0;
        for (Seccion s : seccionesActivas()) {
            int alumnos = accesoService.alumnosDe(s).size();
            for (SeccionCurso sc : cursosActivos(s)) {
                List<Actividad> acts = actividadRepository.findBySeccionCursoIdOrderByFechaLimiteDesc(sc.getId()).stream()
                        .filter(a -> !a.getFechaLimite().isBefore(ini) && a.getFechaLimite().isBefore(fin)).toList();
                long entregadas = 0, porCalificar = 0;
                for (Actividad a : acts) {
                    entregadas += entregaRepository.countByActividadId(a.getId());
                    porCalificar += entregaRepository.countByActividadIdAndEstado(a.getId(), Entrega.ENTREGADA);
                }
                long esperadas = (long) acts.size() * alumnos;
                totalEsperadas += esperadas;
                totalEntregadas += entregadas;
                totalPorCalificar += porCalificar;
                totalActividades += acts.size();
                filas.add(new ArrayList<>(List.of(s.getGrado(), s.getLetra(), sc.getCurso().getNombre(),
                        sc.getDocente() == null ? "Sin docente" : sc.getDocente().getNombreCompleto(), acts.size(),
                        esperadas, entregadas, porCalificar, celda(porcentaje(entregadas, esperadas)))));
            }
        }
        return new ReporteDto.Tabla("entregas", "Cumplimiento de actividades", p.getNombre()
                + " · Actividades con fecha límite dentro del periodo · Generado el " + hoy(),
                List.of("Grado", "Sección", "Curso", "Docente", "Actividades", "Entregas esperadas", "Entregadas",
                        "Por calificar", "% Cumplimiento"),
                filas, List.of(
                        new ReporteDto.Indicador("Actividades", String.valueOf(totalActividades), null),
                        new ReporteDto.Indicador("Cumplimiento", textoPct(porcentaje(totalEntregadas, totalEsperadas)), null),
                        new ReporteDto.Indicador("Entregas por calificar", String.valueOf(totalPorCalificar), null)));
    }

    // ------------------------------------------------------------------

    private Periodo periodo(Long periodoId) {
        Periodo p = notaService.elegirPeriodo(periodoId);
        if (p == null) throw new DatosInvalidosException("Aún no hay periodos registrados. Créalos en Académico > Periodos.");
        return p;
    }

    private Seccion seccion(Long id) {
        return seccionRepository.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Sección no encontrada."));
    }

    private List<Seccion> seccionesActivas() {
        return seccionRepository.findAll().stream()
                .filter(s -> "Activa".equals(s.getEstado()))
                .sorted(Comparator.comparingInt((Seccion s) -> Grados.orden(s.getGrado())).thenComparing(Seccion::getLetra))
                .toList();
    }

    private List<SeccionCurso> cursosActivos(Seccion s) {
        return seccionCursoRepository.findBySeccionId(s.getId()).stream()
                .filter(sc -> "Activo".equals(sc.getEstado()))
                .sorted(Comparator.comparing(sc -> sc.getCurso().getNombre(), String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    /** Puesto por promedio (empates comparten puesto). */
    private static Map<Long, Integer> puestos(Map<Long, Double> generales) {
        List<Map.Entry<Long, Double>> lista = generales.entrySet().stream().filter(e -> e.getValue() != null)
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed()).toList();
        Map<Long, Integer> res = new HashMap<>();
        int puesto = 0;
        Double anterior = null;
        for (int i = 0; i < lista.size(); i++) {
            if (!lista.get(i).getValue().equals(anterior)) puesto = i + 1;
            anterior = lista.get(i).getValue();
            res.put(lista.get(i).getKey(), puesto);
        }
        return res;
    }

    private static Double porcentaje(long parte, long total) {
        return total == 0 ? null : Calificacion.dosDecimales(parte * 100.0 / total);
    }

    /** null -> "" para que la celda quede vacía. */
    private static Object celda(Object v) {
        return v == null ? "" : v;
    }

    private static String texto(Double v) {
        return v == null ? "—" : String.format(Locale.US, "%.2f", v);
    }

    private static String textoPct(Double v) {
        return v == null ? "—" : String.format(Locale.US, "%.1f %%", v);
    }

    private static String hoy() {
        return LocalDate.now().format(DMY);
    }
}
