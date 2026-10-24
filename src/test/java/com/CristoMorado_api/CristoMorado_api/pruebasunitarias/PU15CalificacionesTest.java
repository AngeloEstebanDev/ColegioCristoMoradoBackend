package com.CristoMorado_api.CristoMorado_api.pruebasunitarias;

import com.CristoMorado_api.CristoMorado_api.dto.NotaDto;
import com.CristoMorado_api.CristoMorado_api.entity.*;
import com.CristoMorado_api.CristoMorado_api.exception.DatosInvalidosException;
import com.CristoMorado_api.CristoMorado_api.repository.*;
import com.CristoMorado_api.CristoMorado_api.services.AccesoService;
import com.CristoMorado_api.CristoMorado_api.services.NotaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** PU-15 – HU-16 y HU-17: Calificaciones (Épica 5, Sprint 5). NotaService.guardarNotas (escala 0 a 20). */
@DisplayName("PU-15 – HU-16 y HU-17: Calificaciones")
class PU15CalificacionesTest {

    private static final Long DOCENTE = 7L, CURSO = 64L;

    private NotaRepository notaRepository;
    private NotaHistorialRepository historialRepository;
    private NotaService servicio;
    private Evaluacion examen;
    private Estudiante juan;

    @BeforeEach
    void setUp() {
        EvaluacionRepository evaluacionRepository = mock(EvaluacionRepository.class);
        notaRepository = mock(NotaRepository.class);
        historialRepository = mock(NotaHistorialRepository.class);
        EstudianteRepository estudianteRepository = mock(EstudianteRepository.class);
        AccesoService accesoService = mock(AccesoService.class);
        servicio = new NotaService(evaluacionRepository, notaRepository, historialRepository, mock(PeriodoRepository.class),
                estudianteRepository, mock(ActividadRepository.class), mock(SeccionCursoRepository.class),
                mock(AsistenciaRepository.class), accesoService, mock(EntregaRepository.class));
        Seccion quintoA = Seccion.builder().id(8L).grado("5° Primaria").letra("A").build();
        SeccionCurso matematica = SeccionCurso.builder().id(CURSO).seccion(quintoA)
                .curso(Curso.builder().id(1L).nombre("Matemática").build()).build();
        examen = Evaluacion.builder().id(20L).seccionCurso(matematica).nombre("Examen bimestral").build();
        juan = Estudiante.builder().id(1L).nombreCompleto("Juan Pérez García").grado("5° Primaria").seccion("A").build();
        when(accesoService.cursoDelDocente(DOCENTE, CURSO)).thenReturn(matematica);
        when(accesoService.usuario(DOCENTE)).thenReturn(Usuario.builder().id(DOCENTE).nombre("Carlos Mendoza Ruiz").build());
        when(evaluacionRepository.findById(20L)).thenReturn(Optional.of(examen));
        when(estudianteRepository.findById(1L)).thenReturn(Optional.of(juan));
    }

    private static NotaDto.GuardarNotasRequest nota(Double valor, String motivo) {
        return new NotaDto.GuardarNotasRequest(List.of(new NotaDto.Cambio(20L, 1L, valor, null)), motivo);
    }

    @Test
    @DisplayName("PU-15.1 Nota dentro del rango configurado (0 a 20): es aceptada")
    void c1_notaEnRango() {
        servicio.guardarNotas(DOCENTE, CURSO, nota(16.0, null));
        servicio.guardarNotas(DOCENTE, CURSO, nota(0.0, null));
        servicio.guardarNotas(DOCENTE, CURSO, nota(20.0, null));

        verify(notaRepository, times(3)).save(any(Nota.class));
    }

    @Test
    @DisplayName("PU-15.2 Nota inferior al mínimo (-1) o superior al máximo (21): es rechazada")
    void c2_notaFueraDeRango() {
        assertEquals("Las notas van de 0 a 20.", assertThrows(DatosInvalidosException.class,
                () -> servicio.guardarNotas(DOCENTE, CURSO, nota(-1.0, null))).getMessage());
        assertEquals("Las notas van de 0 a 20.", assertThrows(DatosInvalidosException.class,
                () -> servicio.guardarNotas(DOCENTE, CURSO, nota(21.0, null))).getMessage());
        verify(notaRepository, never()).save(any(Nota.class));
    }

    @Test
    @DisplayName("PU-15.3 Actualización válida de una calificación (12 → 15): los datos procesados son correctos")
    void c3_actualizacionValida() {
        Nota actual = Nota.builder().id(40L).evaluacion(examen).estudiante(juan).valor(12.0).build();
        when(notaRepository.findByEvaluacionIdAndEstudianteId(20L, 1L)).thenReturn(Optional.of(actual));

        servicio.guardarNotas(DOCENTE, CURSO, nota(15.0, "Corrección de suma"));

        assertEquals(15.0, actual.getValor().doubleValue(), 0.001);
        assertNotNull(actual.getFechaActualizacion());
        verify(historialRepository, times(1)).save(any(NotaHistorial.class));
    }
}
