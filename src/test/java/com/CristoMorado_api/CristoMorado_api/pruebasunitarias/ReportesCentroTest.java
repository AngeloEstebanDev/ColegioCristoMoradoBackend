package com.CristoMorado_api.CristoMorado_api.pruebasunitarias;

import com.CristoMorado_api.CristoMorado_api.dto.ReporteDto;
import com.CristoMorado_api.CristoMorado_api.entity.*;
import com.CristoMorado_api.CristoMorado_api.exception.DatosInvalidosException;
import com.CristoMorado_api.CristoMorado_api.repository.*;
import com.CristoMorado_api.CristoMorado_api.services.AccesoService;
import com.CristoMorado_api.CristoMorado_api.services.NotaService;
import com.CristoMorado_api.CristoMorado_api.services.ReporteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Centro de Control de Reportes (HU-30 y HU-31): exportar notas de una sección,
 * reporte de notas por alumno y reporte de asistencia por alumno.
 */
@DisplayName("RC – Centro de Control de Reportes")
class ReportesCentroTest {

    private ReporteService servicio;
    private NotaService notaService;
    private EvaluacionRepository evaluacionRepository;
    private AsistenciaRepository asistenciaRepository;
    private Periodo bim1, bim2;
    private Estudiante juan, maria;
    private SeccionCurso mat, com;

    @BeforeEach
    void setUp() {
        SeccionRepository seccionRepository = mock(SeccionRepository.class);
        SeccionCursoRepository seccionCursoRepository = mock(SeccionCursoRepository.class);
        evaluacionRepository = mock(EvaluacionRepository.class);
        asistenciaRepository = mock(AsistenciaRepository.class);
        AccesoService accesoService = mock(AccesoService.class);
        notaService = mock(NotaService.class);
        servicio = new ReporteService(seccionRepository, seccionCursoRepository, evaluacionRepository, asistenciaRepository,
                mock(ActividadRepository.class), mock(EntregaRepository.class), accesoService, notaService);

        bim1 = Periodo.builder().id(1L).nombre("I Bimestre 2026").anio(2026).bimestre(1)
                .fechaInicio(LocalDate.of(2026, 3, 16)).fechaFin(LocalDate.of(2026, 5, 15)).build();
        bim2 = Periodo.builder().id(2L).nombre("II Bimestre 2026").anio(2026).bimestre(2)
                .fechaInicio(LocalDate.of(2026, 5, 25)).fechaFin(LocalDate.of(2026, 7, 24)).build();
        when(notaService.periodosOrdenados()).thenReturn(List.of(bim2, bim1));
        when(notaService.elegirPeriodo(1L)).thenReturn(bim1);

        Seccion quintoA = Seccion.builder().id(8L).grado("5° Primaria").letra("A").estado("Activa").build();
        Seccion sextoA = Seccion.builder().id(9L).grado("6° Primaria").letra("A").estado("Activa").build();
        when(seccionRepository.findAll()).thenReturn(List.of(sextoA, quintoA));
        when(seccionRepository.findById(8L)).thenReturn(Optional.of(quintoA));
        mat = SeccionCurso.builder().id(64L).seccion(quintoA).curso(Curso.builder().id(1L).nombre("Matemática").build()).build();
        com = SeccionCurso.builder().id(65L).seccion(quintoA).curso(Curso.builder().id(2L).nombre("Comunicación").build()).build();
        when(seccionCursoRepository.findBySeccionId(8L)).thenReturn(List.of(mat, com));
        juan = Estudiante.builder().id(1L).nombreCompleto("Juan Pérez García").grado("5° Primaria").seccion("A").build();
        maria = Estudiante.builder().id(2L).nombreCompleto("María López Ríos").grado("5° Primaria").seccion("A").build();
        when(accesoService.alumnosDe(quintoA)).thenReturn(List.of(juan, maria));
    }

    @Test
    @DisplayName("RC-01 Opciones: grados, secciones, cursos y alumnos para los filtros")
    void c1_opciones() {
        ReporteDto.Opciones o = servicio.opciones();

        assertEquals(List.of("5° Primaria", "6° Primaria"), o.grados());
        assertEquals("5° Primaria A", o.secciones().get(0).nombre());
        assertEquals(List.of("Comunicación", "Matemática"), o.cursos().stream().map(ReporteDto.Opcion::nombre).toList());
        assertEquals(2, o.alumnos().size());
        assertEquals(8L, o.alumnos().get(0).seccionId());
    }

    @Test
    @DisplayName("RC-02 Exportar notas: registro de una sección y opción «Todos» en grado, sección y periodo")
    void c2_exportarNotas() {
        when(notaService.promediosDelCurso(mat, bim1)).thenReturn(Map.of(1L, 16.0, 2L, 9.0));
        when(notaService.promediosDelCurso(com, bim1)).thenReturn(Map.of(1L, 14.0));

        ReporteDto.Tabla t = servicio.generar("notas", 1L, 8L, null, null, "5° Primaria", null, null);
        assertEquals("Registro de notas - 5° Primaria A", t.titulo());
        assertEquals(2, t.filas().size());

        // "Todas las secciones" de un grado con una sola sección: mismo registro clásico.
        ReporteDto.Tabla grado = servicio.generar("notas", 1L, null, null, null, "5° Primaria", null, null);
        assertEquals("Registro de notas - 5° Primaria A", grado.titulo());
        assertEquals(2, grado.filas().size());

        // Todos los grados, secciones y periodos: se unen los registros con Grado, Sección y Período al inicio.
        ReporteDto.Tabla todo = servicio.generar("notas", null, null, null, null, null, null, null);
        assertEquals("Registro de notas - Todos los grados", todo.titulo());
        assertEquals(List.of("Grado", "Sección", "Período", "N°", "Alumno"), todo.columnas().subList(0, 5));
        assertTrue(todo.columnas().containsAll(List.of("Matemática", "Comunicación", "Promedio", "Situación")));
        assertEquals(4, todo.filas().size());                       // 2 alumnos x 2 bimestres (6° A no tiene alumnos)
        assertEquals(List.of("5° Primaria", "A", "I Bimestre"), todo.filas().get(0).subList(0, 3));
        assertEquals("II Bimestre", todo.filas().get(2).get(2));
        assertTrue(assertThrows(DatosInvalidosException.class,
                () -> servicio.generar("notas", 1L, 8L, null, null, "6° Primaria", null, null)).getMessage().contains("no pertenece"));
    }

    @Test
    @DisplayName("RC-03 Notas por alumno: una fila por nota, filtrando por alumno y curso")
    void c3_notasPorAlumno() {
        Evaluacion ex1 = Evaluacion.builder().id(20L).seccionCurso(mat).periodo(bim1).nombre("Examen mensual").fecha(LocalDate.of(2026, 4, 10)).build();
        Evaluacion ta1 = Evaluacion.builder().id(21L).seccionCurso(com).periodo(bim1).nombre("Tarea 1").fecha(LocalDate.of(2026, 4, 2)).build();
        Evaluacion ex2 = Evaluacion.builder().id(22L).seccionCurso(mat).periodo(bim2).nombre("Examen bimestral").fecha(LocalDate.of(2026, 7, 1)).build();
        when(evaluacionRepository.findBySeccionCursoIdIn(any())).thenReturn(List.of(ex1, ta1, ex2));
        when(notaService.notasDe(any())).thenAnswer(i -> {
            java.util.Collection<Long> ids = i.getArgument(0);
            return List.of(
                    Nota.builder().id(1L).evaluacion(ex1).estudiante(juan).valor(15.0).build(),
                    Nota.builder().id(2L).evaluacion(ta1).estudiante(juan).valor(17.0).build(),
                    Nota.builder().id(3L).evaluacion(ex1).estudiante(maria).valor(9.0).build(),
                    Nota.builder().id(4L).evaluacion(ex2).estudiante(juan).valor(18.0).build())
                    .stream().filter(n -> ids.contains(n.getEvaluacion().getId())).toList();
        });

        ReporteDto.Tabla todo = servicio.generar("notas-alumno", null, null, null, null, null, null, null);
        assertEquals(List.of("Grado", "Sección", "Alumno", "Período", "Curso", "Actividad / Evaluación", "Nota"), todo.columnas());
        assertEquals(4, todo.filas().size());
        assertEquals(List.of("5° Primaria", "A", "Juan Pérez García", "I Bimestre", "Comunicación", "Tarea 1", 17.0), todo.filas().get(0));

        ReporteDto.Tabla filtrado = servicio.generar("notas-alumno", 1L, 8L, null, null, "5° Primaria", 1L, 1L);
        assertEquals(1, filtrado.filas().size());
        assertEquals("Examen mensual", filtrado.filas().get(0).get(5));
        assertEquals(15.0, filtrado.filas().get(0).get(6));
    }

    @Test
    @DisplayName("RC-04 Asistencia por alumno: fecha, estado en texto e indicadores")
    void c4_asistenciaPorAlumno() {
        when(asistenciaRepository.findBySeccionCursoIdAndFechaBetween(eq(64L), any(), any())).thenReturn(List.of(
                Asistencia.builder().id(1L).seccionCurso(mat).estudiante(juan).fecha(LocalDate.of(2026, 3, 18)).estado("PRESENTE").build(),
                Asistencia.builder().id(2L).seccionCurso(mat).estudiante(juan).fecha(LocalDate.of(2026, 3, 20)).estado("TARDANZA").build(),
                Asistencia.builder().id(3L).seccionCurso(mat).estudiante(maria).fecha(LocalDate.of(2026, 3, 18)).estado("FALTA").build(),
                Asistencia.builder().id(4L).seccionCurso(mat).estudiante(juan).fecha(LocalDate.of(2026, 5, 20)).estado("PRESENTE").build()));

        ReporteDto.Tabla t = servicio.generar("asistencia-alumno", null, 8L, null, null, null, null, null);
        assertEquals(3, t.filas().size(), "El 20/05 está entre bimestres: no se incluye");
        assertEquals(List.of("5° Primaria", "A", "Juan Pérez García", "I Bimestre", "Matemática", "18/03/2026", "Asistió"), t.filas().get(0));
        assertEquals("Tardanza", t.filas().get(1).get(6));
        assertEquals("Falta", t.filas().get(2).get(6));
        assertEquals("66.7 %", t.indicadores().stream().filter(i -> i.etiqueta().equals("% asistencia")).findFirst().orElseThrow().valor());

        ReporteDto.Tabla soloMaria = servicio.generar("asistencia-alumno", 1L, 8L, null, null, "5° Primaria", 2L, null);
        assertEquals(1, soloMaria.filas().size());
    }

    @Test
    @DisplayName("RC-05 Filtros inválidos: grado inexistente o sección de otro grado")
    void c5_filtrosInvalidos() {
        assertEquals("Grado no válido: 9° Primaria", assertThrows(DatosInvalidosException.class,
                () -> servicio.generar("notas-alumno", null, null, null, null, "9° Primaria", null, null)).getMessage());
        assertTrue(assertThrows(DatosInvalidosException.class,
                () -> servicio.generar("asistencia-alumno", null, 8L, null, null, "6° Primaria", null, null)).getMessage().contains("no pertenece"));
    }
}
