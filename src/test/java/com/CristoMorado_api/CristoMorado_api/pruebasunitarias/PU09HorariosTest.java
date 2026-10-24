package com.CristoMorado_api.CristoMorado_api.pruebasunitarias;

import com.CristoMorado_api.CristoMorado_api.dto.HorarioDto;
import com.CristoMorado_api.CristoMorado_api.entity.*;
import com.CristoMorado_api.CristoMorado_api.exception.DatosInvalidosException;
import com.CristoMorado_api.CristoMorado_api.repository.*;
import com.CristoMorado_api.CristoMorado_api.services.HorarioService;
import com.CristoMorado_api.CristoMorado_api.services.MensajeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * PU-09 – HU-22: Gestión de horarios (Épica 3, Sprint 3). HorarioService.guardar.
 * El docente Carlos Mendoza dicta Matemática en 5° Primaria A y Ciencia y Tecnología en 4° Primaria A
 * (ambas en el turno mañana). En 4° A ya tiene clase el lunes de 08:30 a 10:00.
 */
@DisplayName("PU-09 – HU-22: Gestión de horarios")
class PU09HorariosTest {

    private HorarioBloqueRepository bloqueRepository;
    private HorarioService servicio;

    @BeforeEach
    void setUp() {
        SeccionRepository seccionRepository = mock(SeccionRepository.class);
        SeccionCursoRepository seccionCursoRepository = mock(SeccionCursoRepository.class);
        bloqueRepository = mock(HorarioBloqueRepository.class);
        servicio = new HorarioService(seccionRepository, seccionCursoRepository, bloqueRepository,
                mock(EstudianteRepository.class), mock(MensajeService.class));

        Docente carlos = Docente.builder().id(1L).nombreCompleto("Carlos Mendoza Ruiz").usuarioId(7L).build();
        Seccion quintoA = Seccion.builder().id(8L).grado("5° Primaria").letra("A").estado("Activa").build();
        Seccion cuartoA = Seccion.builder().id(6L).grado("4° Primaria").letra("A").estado("Activa").build();
        SeccionCurso matematica = SeccionCurso.builder().id(64L).seccion(quintoA).docente(carlos)
                .curso(Curso.builder().id(1L).nombre("Matemática").build()).estado("Activo").build();
        SeccionCurso ciencia = SeccionCurso.builder().id(50L).seccion(cuartoA).docente(carlos)
                .curso(Curso.builder().id(3L).nombre("Ciencia y Tecnología").build()).estado("Activo").build();
        when(seccionRepository.findById(8L)).thenReturn(Optional.of(quintoA));
        when(seccionCursoRepository.findById(64L)).thenReturn(Optional.of(matematica));
        when(seccionCursoRepository.findByDocenteUsuarioId(7L)).thenReturn(List.of(matematica, ciencia));
        when(bloqueRepository.findBySeccionCursoId(50L)).thenReturn(List.of(HorarioBloque.builder().id(900L)
                .seccion(cuartoA).seccionCurso(ciencia).dia(1).horaInicio(LocalTime.of(8, 30)).horaFin(LocalTime.of(10, 0)).build()));
    }

    private static HorarioDto.GuardarRequest bloque(int dia, String inicio, String fin) {
        return new HorarioDto.GuardarRequest(List.of(new HorarioDto.BloqueRequest(64L, dia, inicio, fin)));
    }

    @Test
    @DisplayName("PU-09.1 Dos clases del mismo docente en horarios superpuestos (otra sección): se detecta el cruce")
    void c1_cruceDocente() {
        DatosInvalidosException ex = assertThrows(DatosInvalidosException.class,
                () -> servicio.guardar(8L, bloque(1, "08:30", "10:00")),
                "Debe detectar que Carlos Mendoza ya tiene clase en 4° Primaria A el lunes de 08:30 a 10:00");

        assertTrue(ex.getMessage().contains("Carlos Mendoza Ruiz"), ex.getMessage());
        verify(bloqueRepository, never()).save(any(HorarioBloque.class));
    }

    @Test
    @DisplayName("PU-09.2 Dos clases del mismo docente en horarios diferentes: no se detecta cruce")
    void c2_sinCruce() {
        servicio.guardar(8L, bloque(2, "08:30", "10:00"));   // martes: Carlos está libre

        verify(bloqueRepository, times(1)).save(any(HorarioBloque.class));
    }

    @Test
    @DisplayName("PU-09.3 Hora de inicio posterior a la hora de fin: la validación falla")
    void c3_inicioPosterior() {
        DatosInvalidosException ex = assertThrows(DatosInvalidosException.class,
                () -> servicio.guardar(8L, bloque(3, "10:00", "08:30")));

        assertTrue(ex.getMessage().endsWith("la hora fin debe ser mayor que la de inicio."), ex.getMessage());
        verify(bloqueRepository, never()).save(any(HorarioBloque.class));
    }
}
