package com.CristoMorado_api.CristoMorado_api.services;

import com.CristoMorado_api.CristoMorado_api.dto.AsistenciaDto;
import com.CristoMorado_api.CristoMorado_api.entity.*;
import com.CristoMorado_api.CristoMorado_api.exception.AccesoDenegadoException;
import com.CristoMorado_api.CristoMorado_api.exception.DatosInvalidosException;
import com.CristoMorado_api.CristoMorado_api.repository.AsistenciaRepository;
import com.CristoMorado_api.CristoMorado_api.repository.EstudianteRepository;
import com.CristoMorado_api.CristoMorado_api.repository.HorarioBloqueRepository;
import com.CristoMorado_api.CristoMorado_api.repository.PeriodoRepository;
import com.CristoMorado_api.CristoMorado_api.repository.SesionAsistenciaRepository;
import com.CristoMorado_api.CristoMorado_api.util.Calificacion;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * HU-18 Registro de asistencia: el docente pasa lista de su curso por fecha
 * (Presente, Tardanza, Falta, Justificada). Los padres reciben un aviso cuando
 * se registra una falta. Alumnos y padres consultan el resumen.
 *
 * Antes de tomar lista, el docente "genera" la asistencia de la clase: solo puede hacerlo
 * durante la hora de clase (según el horario del curso). Una clase que pasó sin generarse
 * queda como "No generada" y ya no se puede registrar.
 */
@Service
public class AsistenciaService {

    private static final Set<String> ESTADOS = Set.of(
            Asistencia.PRESENTE, Asistencia.TARDANZA, Asistencia.FALTA, Asistencia.JUSTIFICADA);
    private static final DateTimeFormatter DMY = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm");

    private final AsistenciaRepository asistenciaRepository;
    private final EstudianteRepository estudianteRepository;
    private final AccesoService accesoService;
    private final MensajeService mensajeService;
    private final SesionAsistenciaRepository sesionRepository;
    private final HorarioBloqueRepository bloqueRepository;
    private final PeriodoRepository periodoRepository;

    public AsistenciaService(AsistenciaRepository asistenciaRepository, EstudianteRepository estudianteRepository,
                             AccesoService accesoService, MensajeService mensajeService,
                             SesionAsistenciaRepository sesionRepository, HorarioBloqueRepository bloqueRepository,
                             PeriodoRepository periodoRepository) {
        this.asistenciaRepository = asistenciaRepository;
        this.estudianteRepository = estudianteRepository;
        this.accesoService = accesoService;
        this.mensajeService = mensajeService;
        this.sesionRepository = sesionRepository;
        this.bloqueRepository = bloqueRepository;
        this.periodoRepository = periodoRepository;
    }

    /** Lista del día para pasar asistencia. */
    @Transactional(readOnly = true)
    public AsistenciaDto.Dia dia(Long usuarioId, Long seccionCursoId, LocalDate fecha) {
        SeccionCurso sc = cursoParaLeer(usuarioId, seccionCursoId);
        return construirDia(sc, fecha == null ? LocalDate.now() : fecha);
    }

    @Transactional
    public AsistenciaDto.Dia guardar(Long usuarioId, Long seccionCursoId, AsistenciaDto.GuardarRequest req) {
        SeccionCurso sc = accesoService.cursoDelDocente(usuarioId, seccionCursoId);
        if (req == null || req.fecha() == null) throw new DatosInvalidosException("Indica la fecha.");
        LocalDate fecha = req.fecha();
        if (fecha.isAfter(LocalDate.now())) throw new DatosInvalidosException("No se puede registrar asistencia de una fecha futura.");
        if (fecha.getDayOfWeek().getValue() > 6) throw new DatosInvalidosException("No hay clases los domingos.");
        if (req.registros() == null || req.registros().isEmpty()) throw new DatosInvalidosException("Marca la asistencia de al menos un alumno.");
        boolean generada = sesionRepository.findBySeccionCursoIdAndFecha(sc.getId(), fecha).isPresent()
                || !asistenciaRepository.findBySeccionCursoIdAndFecha(sc.getId(), fecha).isEmpty();
        if (!generada) {
            throw new DatosInvalidosException("Primero genera la asistencia de esa clase (solo durante la hora de clase).");
        }
        // La asistencia se cierra cuando termina la clase: después ya no se puede editar.
        LocalDateTime ahora = LocalDateTime.now();
        Clase clase = claseDelDia(sc, fecha);
        if (!fecha.equals(ahora.toLocalDate()) || clase == null || !ahora.toLocalTime().isBefore(clase.fin())) {
            throw new DatosInvalidosException("La asistencia del " + fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                    + " ya está cerrada: solo se puede registrar o editar durante la hora de clase"
                    + (clase == null ? "." : " (hasta las " + clase.fin().format(HHMM) + ")."));
        }

        Map<Long, Asistencia> existentes = asistenciaRepository.findBySeccionCursoIdAndFecha(sc.getId(), fecha).stream()
                .collect(Collectors.toMap(a -> a.getEstudiante().getId(), a -> a, (a, b) -> a));
        Set<Long> delSalon = accesoService.alumnosDe(sc.getSeccion()).stream().map(Estudiante::getId).collect(Collectors.toSet());

        List<Estudiante> nuevasFaltas = new ArrayList<>();
        for (AsistenciaDto.Registro r : req.registros()) {
            if (r == null || r.estudianteId() == null) continue;
            if (r.estado() == null || r.estado().isBlank()) continue;
            String estado = estadoValido(r.estado());
            Asistencia a = existentes.get(r.estudianteId());
            if (a == null && !delSalon.contains(r.estudianteId())) {
                throw new DatosInvalidosException("Un alumno de la lista no pertenece a esta sección.");
            }
            String obs = NotaService.recortar(r.observacion(), 300);
            if (a == null) {
                Estudiante e = estudianteRepository.findById(r.estudianteId())
                        .orElseThrow(() -> new DatosInvalidosException("Alumno no encontrado."));
                asistenciaRepository.save(Asistencia.builder().seccionCurso(sc).estudiante(e).fecha(fecha)
                        .estado(estado).observacion(obs).registradoPor(usuarioId).build());
                if (Asistencia.FALTA.equals(estado)) nuevasFaltas.add(e);
            } else {
                if (!Asistencia.FALTA.equals(a.getEstado()) && Asistencia.FALTA.equals(estado)) nuevasFaltas.add(a.getEstudiante());
                a.setEstado(estado);
                a.setObservacion(obs);
                a.setRegistradoPor(usuarioId);
                a.setFechaRegistro(LocalDateTime.now());
                asistenciaRepository.save(a);
            }
        }
        avisarFaltas(sc, fecha, nuevasFaltas);
        return construirDia(sc, fecha);
    }

    /**
     * Estado de asistencia normalizado (PRESENTE, TARDANZA, FALTA o JUSTIFICADA).
     * Lanza DatosInvalidosException si no es uno de los permitidos (PU-13).
     */
    public static String estadoValido(String estado) {
        String e = estado == null ? "" : estado.trim().toUpperCase();
        if (!ESTADOS.contains(e)) throw new DatosInvalidosException("Estado de asistencia no válido: " + estado);
        return e;
    }

    // ------------------------------------------------------------------
    // Sesiones (generar asistencia en la hora de clase)
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public AsistenciaDto.Sesiones sesiones(Long usuarioId, Long seccionCursoId) {
        SeccionCurso sc = cursoParaLeer(usuarioId, seccionCursoId);
        return construirSesiones(sc, LocalDateTime.now());
    }

    @Transactional
    public AsistenciaDto.Sesiones generar(Long usuarioId, Long seccionCursoId) {
        SeccionCurso sc = accesoService.cursoDelDocente(usuarioId, seccionCursoId);
        LocalDateTime ahora = LocalDateTime.now();
        LocalDate hoy = ahora.toLocalDate();
        Clase clase = claseDelDia(sc, hoy);
        if (clase == null) {
            throw new DatosInvalidosException("Hoy no hay clase de " + sc.getCurso().getNombre() + " en el horario.");
        }
        LocalTime t = ahora.toLocalTime();
        if (t.isBefore(clase.inicio()) || !t.isBefore(clase.fin())) {
            throw new DatosInvalidosException("Solo puedes generar la asistencia durante la hora de clase (hoy de "
                    + clase.inicio().format(HHMM) + " a " + clase.fin().format(HHMM) + ").");
        }
        if (sesionRepository.findBySeccionCursoIdAndFecha(sc.getId(), hoy).isPresent()) {
            throw new DatosInvalidosException("La asistencia de hoy ya fue generada.");
        }
        sesionRepository.save(SesionAsistencia.builder().seccionCurso(sc).fecha(hoy).horaInicio(clase.inicio())
                .generadaPor(usuarioId).build());
        return construirSesiones(sc, ahora);
    }

    private record Clase(LocalTime inicio, LocalTime fin) {}

    /** Primera y última hora del curso ese día según el horario (null si no hay clase). */
    private Clase claseDelDia(SeccionCurso sc, LocalDate fecha) {
        return claseDelDia(bloqueRepository.findBySeccionCursoId(sc.getId()), fecha);
    }

    private static Clase claseDelDia(List<HorarioBloque> bloques, LocalDate fecha) {
        int dia = fecha.getDayOfWeek().getValue();
        LocalTime ini = null, fin = null;
        for (HorarioBloque b : bloques) {
            if (b.getDia() == null || b.getDia() != dia) continue;
            if (ini == null || b.getHoraInicio().isBefore(ini)) ini = b.getHoraInicio();
            if (fin == null || b.getHoraFin().isAfter(fin)) fin = b.getHoraFin();
        }
        return ini == null ? null : new Clase(ini, fin);
    }

    private AsistenciaDto.Sesiones construirSesiones(SeccionCurso sc, LocalDateTime ahora) {
        LocalDate hoy = ahora.toLocalDate();
        List<HorarioBloque> bloques = bloqueRepository.findBySeccionCursoId(sc.getId());
        Map<LocalDate, SesionAsistencia> generadas = sesionRepository.findBySeccionCursoId(sc.getId()).stream()
                .collect(Collectors.toMap(SesionAsistencia::getFecha, x -> x, (a, b) -> a));
        Map<LocalDate, Long> registros = asistenciaRepository.findBySeccionCursoId(sc.getId()).stream()
                .collect(Collectors.groupingBy(Asistencia::getFecha, Collectors.counting()));

        // Días de clase: desde el inicio de los bimestres del año hasta hoy (solo dentro de algún bimestre).
        List<Periodo> periodos = periodoRepository.findAll().stream().filter(p -> p.getAnio() == hoy.getYear()).toList();
        TreeSet<LocalDate> fechas = new TreeSet<>(Comparator.reverseOrder());
        fechas.addAll(generadas.keySet());
        fechas.addAll(registros.keySet());
        for (Periodo p : periodos) {
            for (LocalDate d = p.getFechaInicio(); !d.isAfter(p.getFechaFin()) && !d.isAfter(hoy); d = d.plusDays(1)) {
                if (claseDelDia(bloques, d) != null) fechas.add(d);
            }
        }

        List<AsistenciaDto.Sesion> lista = new ArrayList<>();
        long marcadas = 0, sinMarcar = 0, noGeneradas = 0;
        for (LocalDate d : fechas) {
            Clase c = claseDelDia(bloques, d);
            SesionAsistencia ses = generadas.get(d);
            long reg = registros.getOrDefault(d, 0L);
            String ini = ses != null ? ses.getHoraInicio().format(HHMM) : c == null ? "" : c.inicio().format(HHMM);
            String fin = c == null ? "" : c.fin().format(HHMM);
            String estado;
            if (reg > 0) { estado = "MARCADA"; marcadas++; }
            else if (ses != null) { estado = "SIN_MARCAR"; sinMarcar++; }
            else if (d.equals(hoy) && c != null && ahora.toLocalTime().isBefore(c.fin())) estado = "HOY";
            else { estado = "NO_GENERADA"; noGeneradas++; }
            boolean editable = d.equals(hoy) && c != null && ahora.toLocalTime().isBefore(c.fin()) && (ses != null || reg > 0);
            lista.add(new AsistenciaDto.Sesion(d, ini, fin, estado, reg, editable));
        }

        Clase hoyClase = claseDelDia(bloques, hoy);
        LocalTime t = ahora.toLocalTime();
        boolean enClase = hoyClase != null && !t.isBefore(hoyClase.inicio()) && t.isBefore(hoyClase.fin());
        boolean yaGenerada = generadas.containsKey(hoy) || registros.containsKey(hoy);
        String mensaje;
        if (hoyClase == null) mensaje = "Hoy no hay clase de este curso.";
        else if (yaGenerada) mensaje = "La asistencia de hoy ya fue generada.";
        else if (enClase) mensaje = "Estás en la hora de clase: ya puedes generar la asistencia.";
        else if (t.isBefore(hoyClase.inicio())) mensaje = "Podrás generar la asistencia hoy de "
                + hoyClase.inicio().format(HHMM) + " a " + hoyClase.fin().format(HHMM) + ".";
        else mensaje = "La clase de hoy ya terminó.";
        AsistenciaDto.Clase actual = enClase ? new AsistenciaDto.Clase(hoy, hoyClase.inicio().format(HHMM),
                hoyClase.fin().format(HHMM)) : null;
        return new AsistenciaDto.Sesiones(sc.getId(), sc.getCurso().getNombre(), sc.getSeccion().getGrado(),
                sc.getSeccion().getLetra(), enClase && !yaGenerada, actual, mensaje,
                marcadas + sinMarcar, marcadas, sinMarcar, noGeneradas, lista);
    }

    /** Resumen del curso entre dos fechas (por defecto, el año en curso). */
    @Transactional(readOnly = true)
    public AsistenciaDto.ResumenCurso resumenCurso(Long usuarioId, Long seccionCursoId, LocalDate desde, LocalDate hasta) {
        SeccionCurso sc = cursoParaLeer(usuarioId, seccionCursoId);
        LocalDate d = desde == null ? LocalDate.now().withDayOfYear(1) : desde;
        LocalDate h = hasta == null ? LocalDate.now() : hasta;
        List<Asistencia> lista = asistenciaRepository.findBySeccionCursoIdAndFechaBetween(sc.getId(), d, h);
        Map<Long, List<Asistencia>> porAlumno = lista.stream().collect(Collectors.groupingBy(a -> a.getEstudiante().getId()));
        LinkedHashMap<Long, String> alumnos = new LinkedHashMap<>();
        accesoService.alumnosDe(sc.getSeccion()).forEach(e -> alumnos.put(e.getId(), e.getNombreCompleto()));
        lista.forEach(a -> alumnos.putIfAbsent(a.getEstudiante().getId(), a.getEstudiante().getNombreCompleto()));
        List<AsistenciaDto.ResumenAlumno> filas = alumnos.entrySet().stream()
                .map(en -> new AsistenciaDto.ResumenAlumno(en.getKey(), en.getValue(),
                        conteo(porAlumno.getOrDefault(en.getKey(), List.of()))))
                .sorted(Comparator.comparing(AsistenciaDto.ResumenAlumno::nombre, String.CASE_INSENSITIVE_ORDER))
                .toList();
        long sesiones = lista.stream().map(Asistencia::getFecha).distinct().count();
        return new AsistenciaDto.ResumenCurso(sc.getId(), sc.getCurso().getNombre(), d, h, sesiones, filas);
    }

    /** Asistencia de un alumno (lo ve él mismo, su padre, sus docentes o la administración). */
    @Transactional(readOnly = true)
    public AsistenciaDto.DelAlumno delAlumno(Long usuarioId, Long estudianteId, LocalDate desde, LocalDate hasta) {
        return construirDelAlumno(accesoService.estudianteVisible(usuarioId, estudianteId), desde, hasta);
    }

    @Transactional(readOnly = true)
    public AsistenciaDto.DelAlumno mia(Long usuarioId, LocalDate desde, LocalDate hasta) {
        return construirDelAlumno(accesoService.alumnoLogeado(usuarioId), desde, hasta);
    }

    /** Presentes + tardanzas + justificadas cuentan como asistencia. */
    public static AsistenciaDto.Conteo conteo(Collection<Asistencia> lista) {
        long p = 0, t = 0, f = 0, j = 0;
        for (Asistencia a : lista) {
            switch (a.getEstado()) {
                case Asistencia.PRESENTE -> p++;
                case Asistencia.TARDANZA -> t++;
                case Asistencia.FALTA -> f++;
                case Asistencia.JUSTIFICADA -> j++;
                default -> { }
            }
        }
        long total = p + t + f + j;
        Double pct = total == 0 ? null : Calificacion.dosDecimales((p + t + j) * 100.0 / total);
        return new AsistenciaDto.Conteo(p, t, f, j, total, pct);
    }

    // ------------------------------------------------------------------

    private AsistenciaDto.Dia construirDia(SeccionCurso sc, LocalDate fecha) {
        Map<Long, Asistencia> registradas = asistenciaRepository.findBySeccionCursoIdAndFecha(sc.getId(), fecha).stream()
                .collect(Collectors.toMap(a -> a.getEstudiante().getId(), a -> a, (a, b) -> a));
        LinkedHashMap<Long, AsistenciaDto.Fila> filas = new LinkedHashMap<>();
        for (Estudiante e : accesoService.alumnosDe(sc.getSeccion())) {
            Asistencia a = registradas.get(e.getId());
            filas.put(e.getId(), new AsistenciaDto.Fila(e.getId(), e.getNombreCompleto(),
                    a == null ? null : a.getEstado(), a == null ? null : a.getObservacion(), NotaService.foto(e)));
        }
        registradas.values().forEach(a -> filas.putIfAbsent(a.getEstudiante().getId(), new AsistenciaDto.Fila(
                a.getEstudiante().getId(), a.getEstudiante().getNombreCompleto(), a.getEstado(), a.getObservacion(),
                NotaService.foto(a.getEstudiante()))));
        List<LocalDate> fechas = asistenciaRepository.findBySeccionCursoId(sc.getId()).stream()
                .map(Asistencia::getFecha).distinct().sorted(Comparator.reverseOrder()).limit(60).toList();
        return new AsistenciaDto.Dia(sc.getId(), sc.getCurso().getNombre(), sc.getSeccion().getGrado(),
                sc.getSeccion().getLetra(), fecha, !registradas.isEmpty(), new ArrayList<>(filas.values()), fechas);
    }

    private AsistenciaDto.DelAlumno construirDelAlumno(Estudiante e, LocalDate desde, LocalDate hasta) {
        LocalDate d = desde == null ? LocalDate.now().withDayOfYear(1) : desde;
        LocalDate h = hasta == null ? LocalDate.now() : hasta;
        List<Asistencia> lista = asistenciaRepository.findByEstudianteIdAndFechaBetween(e.getId(), d, h);
        Map<Long, List<Asistencia>> porCurso = lista.stream().collect(Collectors.groupingBy(a -> a.getSeccionCurso().getId()));
        List<AsistenciaDto.PorCurso> cursos = porCurso.values().stream()
                .map(l -> {
                    SeccionCurso sc = l.get(0).getSeccionCurso();
                    return new AsistenciaDto.PorCurso(sc.getId(), sc.getCurso().getNombre(), sc.getCurso().getColor(), conteo(l));
                })
                .sorted(Comparator.comparing(AsistenciaDto.PorCurso::curso, String.CASE_INSENSITIVE_ORDER))
                .toList();
        List<AsistenciaDto.Detalle> detalle = lista.stream()
                .sorted(Comparator.comparing(Asistencia::getFecha).reversed())
                .map(a -> new AsistenciaDto.Detalle(a.getFecha(), a.getSeccionCurso().getCurso().getNombre(),
                        a.getSeccionCurso().getCurso().getColor(), a.getEstado(), a.getObservacion()))
                .toList();
        return new AsistenciaDto.DelAlumno(e.getId(), e.getNombreCompleto(), AccesoService.salon(e), d, h,
                conteo(lista), cursos, detalle);
    }

    private void avisarFaltas(SeccionCurso sc, LocalDate fecha, List<Estudiante> faltas) {
        for (Estudiante e : faltas) {
            List<Usuario> padres = accesoService.padresDe(e);
            if (padres.isEmpty()) continue;
            String html = "<p>Se registró una <strong>falta</strong> de " + MensajeService.escapar(e.getNombreCompleto())
                    + " en el curso <strong>" + MensajeService.escapar(sc.getCurso().getNombre()) + "</strong> el "
                    + fecha.format(DMY) + ".</p><p>Si la inasistencia tiene justificación, comunícate con el docente.</p>";
            mensajeService.enviarAviso(padres, "Inasistencia registrada", html);
        }
    }

    private SeccionCurso cursoParaLeer(Long usuarioId, Long seccionCursoId) {
        Usuario u = accesoService.usuario(usuarioId);
        SeccionCurso sc = accesoService.seccionCurso(seccionCursoId);
        if (AccesoService.esDocenteDe(u.getId(), sc) || AccesoService.esAdministrativo(u)) return sc;
        throw new AccesoDenegadoException("Este curso no está a tu cargo.");
    }
}
