package com.CristoMorado_api.CristoMorado_api.pruebasunitarias;

import com.CristoMorado_api.CristoMorado_api.entity.*;
import com.CristoMorado_api.CristoMorado_api.exception.DatosInvalidosException;
import com.CristoMorado_api.CristoMorado_api.repository.*;
import com.CristoMorado_api.CristoMorado_api.services.AccesoService;
import com.CristoMorado_api.CristoMorado_api.services.AsistenciaService;
import com.CristoMorado_api.CristoMorado_api.services.MensajeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * PU-13 – HU-18: Registro de asistencia (Épica 4, Sprint 4).
 * Estados permitidos: PRESENTE, TARDANZA, FALTA y JUSTIFICADA (AsistenciaService.estadoValido).
 * La asistencia solo se genera durante la hora de clase del horario (AsistenciaService.generar).
 */
@DisplayName("PU-13 – HU-18: Registro de asistencia")
class PU13AsistenciaTest {

    private static final Long DOCENTE = 7L, CURSO = 64L;

    private SesionAsistenciaRepository sesionRepository;
    private HorarioBloqueRepository bloqueRepository;
    private AsistenciaService servicio;

    @BeforeEach
    void setUp() {
        sesionRepository = mock(SesionAsistenciaRepository.class);
        bloqueRepository = mock(HorarioBloqueRepository.class);
        AccesoService accesoService = mock(AccesoService.class);
        servicio = new AsistenciaService(mock(AsistenciaRepository.class), mock(EstudianteRepository.class), accesoService,
                mock(MensajeService.class), sesionRepository, bloqueRepository, mock(PeriodoRepository.class));
        Seccion quintoA = Seccion.builder().id(8L).grado("5° Primaria").letra("A").build();
        SeccionCurso matematica = SeccionCurso.builder().id(CURSO).seccion(quintoA)
                .curso(Curso.builder().id(1L).nombre("Matemática").build()).build();
        when(accesoService.cursoDelDocente(DOCENTE, CURSO)).thenReturn(matematica);
        // Hoy hay clase de Matemática, pero en un horario en el que NO estamos ahora.
        LocalTime ahora = LocalTime.now();
        LocalTime ini = ahora.isBefore(LocalTime.NOON) ? LocalTime.of(22, 0) : LocalTime.of(1, 0);
        when(bloqueRepository.findBySeccionCursoId(CURSO)).thenReturn(List.of(HorarioBloque.builder().id(1L)
                .seccionCurso(matematica).dia(LocalDate.now().getDayOfWeek().getValue())
                .horaInicio(ini).horaFin(ini.plusMinutes(45)).build()));
    }

    @Test
    @DisplayName("PU-13.1 Estado de asistencia permitido: la validación es exitosa")
    void c1_estadoPermitido() {
        assertEquals("PRESENTE", AsistenciaService.estadoValido("presente"));
        assertEquals("TARDANZA", AsistenciaService.estadoValido(" Tardanza "));
        assertEquals("FALTA", AsistenciaService.estadoValido("FALTA"));
        assertEquals("JUSTIFICADA", AsistenciaService.estadoValido("justificada"));
    }

    @Test
    @DisplayName("PU-13.2 Estado inexistente: se rechaza")
    void c2_estadoInexistente() {
        DatosInvalidosException ex = assertThrows(DatosInvalidosException.class,
                () -> AsistenciaService.estadoValido("DORMIDO"));

        assertEquals("Estado de asistencia no válido: DORMIDO", ex.getMessage());
    }

    @Test
    @DisplayName("PU-13.3 Generar asistencia fuera del horario programado: se deniega la operación")
    void c3_fueraDeHorario() {
        DatosInvalidosException ex = assertThrows(DatosInvalidosException.class, () -> servicio.generar(DOCENTE, CURSO));

        assertTrue(ex.getMessage().startsWith("Solo puedes generar la asistencia durante la hora de clase"), ex.getMessage());
        verify(sesionRepository, never()).save(any(SesionAsistencia.class));
    }
}
