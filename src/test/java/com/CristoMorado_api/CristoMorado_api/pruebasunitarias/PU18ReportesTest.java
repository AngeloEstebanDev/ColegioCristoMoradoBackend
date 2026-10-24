package com.CristoMorado_api.CristoMorado_api.pruebasunitarias;

import com.CristoMorado_api.CristoMorado_api.dto.ReporteDto;
import com.CristoMorado_api.CristoMorado_api.entity.*;
import com.CristoMorado_api.CristoMorado_api.exception.DatosInvalidosException;
import com.CristoMorado_api.CristoMorado_api.exception.RecursoNoEncontradoException;
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

/** PU-18 – HU-30: Generación de reportes (Épica 5, Sprint 5). ReporteService.generar con filtros grado/sección/periodo. */
@DisplayName("PU-18 – HU-30: Generación de reportes")
class PU18ReportesTest {

    private ReporteService servicio;
    private SeccionCursoRepository seccionCursoRepository;
    private NotaService notaService;

    @BeforeEach
    void setUp() {
        SeccionRepository seccionRepository = mock(SeccionRepository.class);
        seccionCursoRepository = mock(SeccionCursoRepository.class);
        AccesoService accesoService = mock(AccesoService.class);
        notaService = mock(NotaService.class);
        servicio = new ReporteService(seccionRepository, seccionCursoRepository, mock(EvaluacionRepository.class),
                mock(AsistenciaRepository.class), mock(ActividadRepository.class), mock(EntregaRepository.class),
                accesoService, notaService);

        Periodo bim1 = Periodo.builder().id(5L).nombre("I Bimestre 2026").anio(2026).bimestre(1)
                .fechaInicio(LocalDate.of(2026, 3, 9)).fechaFin(LocalDate.of(2026, 5, 8)).build();
        when(notaService.elegirPeriodo(5L)).thenReturn(bim1);

        Seccion quintoA = Seccion.builder().id(8L).grado("5° Primaria").letra("A").build();
        Seccion sextoB = Seccion.builder().id(9L).grado("6° Primaria").letra("B").build();
        when(seccionRepository.findById(8L)).thenReturn(Optional.of(quintoA));
        when(seccionRepository.findById(9L)).thenReturn(Optional.of(sextoB));

        SeccionCurso mat = SeccionCurso.builder().id(64L).seccion(quintoA).curso(Curso.builder().id(1L).nombre("Matemática").build()).build();
        SeccionCurso com = SeccionCurso.builder().id(65L).seccion(quintoA).curso(Curso.builder().id(2L).nombre("Comunicación").build()).build();
        SeccionCurso inactivo = SeccionCurso.builder().id(66L).seccion(quintoA).estado("Inactivo")
                .curso(Curso.builder().id(3L).nombre("Taller antiguo").build()).build();
        when(seccionCursoRepository.findBySeccionId(8L)).thenReturn(List.of(mat, com, inactivo));

        Estudiante juan = Estudiante.builder().id(1L).nombreCompleto("Juan Pérez García").grado("5° Primaria").seccion("A").build();
        Estudiante maria = Estudiante.builder().id(2L).nombreCompleto("María López Ríos").grado("5° Primaria").seccion("A").build();
        when(accesoService.alumnosDe(quintoA)).thenReturn(List.of(juan, maria));
        when(notaService.promediosDelCurso(mat, bim1)).thenReturn(Map.of(1L, 16.0, 2L, 9.0));
        when(notaService.promediosDelCurso(com, bim1)).thenReturn(Map.of(1L, 14.0, 2L, 12.0));
    }

    @Test
    @DisplayName("PU-18.1 Filtros válidos (5° Primaria, sección A, I Bimestre): el reporte los aplica")
    void c1_filtrosValidos() {
        ReporteDto.Tabla t = servicio.generar("rendimiento", 5L, 8L, null, null);

        assertEquals("Rendimiento académico - 5° Primaria A", t.titulo());
        assertTrue(t.subtitulo().startsWith("I Bimestre 2026"), "El subtítulo debe indicar el periodo filtrado");
        assertEquals(List.of("N°", "Alumno", "Comunicación", "Matemática", "Promedio", "Literal", "Puesto", "Situación"), t.columnas());
        assertEquals(2, t.filas().size());
        assertEquals("Juan Pérez García", t.filas().get(0).get(1));
        assertEquals("Desaprobado en 1 curso", t.filas().get(1).get(7));
        verify(notaService).elegirPeriodo(5L);
        verify(seccionCursoRepository).findBySeccionId(8L);
    }

    @Test
    @DisplayName("PU-18.2 Sin registros coincidentes: devuelve un resultado vacío controlado")
    void c2_sinCoincidencias() {
        ReporteDto.Tabla t = assertDoesNotThrow(() -> servicio.generar("rendimiento", 5L, 9L, null, null));

        assertEquals("Rendimiento académico - 6° Primaria B", t.titulo());
        assertTrue(t.filas().isEmpty());
        assertEquals("0", t.indicadores().get(0).valor());
    }

    @Test
    @DisplayName("PU-18.3 Parámetros inválidos (tipo, fechas o sección): se rechaza la solicitud")
    void c3_parametrosInvalidos() {
        assertEquals("Tipo de reporte no válido: notas_secretas", assertThrows(DatosInvalidosException.class,
                () -> servicio.generar("notas_secretas", 5L, 8L, null, null)).getMessage());
        assertEquals("La fecha final debe ser posterior a la inicial.", assertThrows(DatosInvalidosException.class,
                () -> servicio.generar("asistencia", null, 8L, LocalDate.of(2026, 10, 20), LocalDate.of(2026, 10, 1))).getMessage());
        assertEquals("Sección no encontrada.", assertThrows(RecursoNoEncontradoException.class,
                () -> servicio.generar("rendimiento", 5L, 999L, null, null)).getMessage());
    }
}
