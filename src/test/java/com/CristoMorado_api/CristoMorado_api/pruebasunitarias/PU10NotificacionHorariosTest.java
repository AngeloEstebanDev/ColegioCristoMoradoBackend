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
 * PU-10 – HU-23: Notificaciones de horarios (Épica 3, Sprint 3).
 * Al guardar un horario con cambios, HorarioService avisa a los alumnos de la sección (MensajeService.enviarAviso).
 */
@DisplayName("PU-10 – HU-23: Notificaciones de horarios")
class PU10NotificacionHorariosTest {

    private MensajeService mensajeService;
    private HorarioService servicio;

    @BeforeEach
    void setUp() {
        SeccionRepository seccionRepository = mock(SeccionRepository.class);
        SeccionCursoRepository seccionCursoRepository = mock(SeccionCursoRepository.class);
        HorarioBloqueRepository bloqueRepository = mock(HorarioBloqueRepository.class);
        EstudianteRepository estudianteRepository = mock(EstudianteRepository.class);
        mensajeService = mock(MensajeService.class);
        servicio = new HorarioService(seccionRepository, seccionCursoRepository, bloqueRepository, estudianteRepository, mensajeService);

        Seccion quintoA = Seccion.builder().id(8L).grado("5° Primaria").letra("A").estado("Activa").build();
        SeccionCurso matematica = SeccionCurso.builder().id(64L).seccion(quintoA)
                .curso(Curso.builder().id(1L).nombre("Matemática").build()).estado("Activo").build();
        when(seccionRepository.findById(8L)).thenReturn(Optional.of(quintoA));
        when(seccionCursoRepository.findById(64L)).thenReturn(Optional.of(matematica));
        // Horario anterior: Matemática el lunes de 07:45 a 08:30.
        when(bloqueRepository.findBySeccionId(8L)).thenReturn(List.of(HorarioBloque.builder().id(1L).seccion(quintoA)
                .seccionCurso(matematica).dia(1).horaInicio(LocalTime.of(7, 45)).horaFin(LocalTime.of(8, 30)).build()));
        Usuario cuentaJuan = Usuario.builder().id(32L).usuario("ALU87654321").estado(true).build();
        when(estudianteRepository.findByGradoAndSeccionOrderByNombreCompletoAsc("5° Primaria", "A"))
                .thenReturn(List.of(Estudiante.builder().id(1L).nombreCompleto("Juan Pérez García").usuario(cuentaJuan).build()));
    }

    @Test
    @DisplayName("PU-10.1 Modificación válida del horario: se genera la notificación «Cambio de horario» para los alumnos")
    void c1_generaNotificacion() {
        servicio.guardar(8L, new HorarioDto.GuardarRequest(List.of(new HorarioDto.BloqueRequest(64L, 1, "08:30", "10:00"))));

        verify(mensajeService, times(1)).enviarAviso(any(), eq("Cambio de horario"), anyString());
    }

    @Test
    @DisplayName("PU-10.2 Datos incompletos de la modificación (sin día): no se genera notificación")
    void c2_datosIncompletos() {
        assertThrows(DatosInvalidosException.class, () -> servicio.guardar(8L,
                new HorarioDto.GuardarRequest(List.of(new HorarioDto.BloqueRequest(64L, null, "08:30", "10:00")))));
        assertThrows(DatosInvalidosException.class, () -> servicio.guardar(8L,
                new HorarioDto.GuardarRequest(List.of(new HorarioDto.BloqueRequest(null, 1, "08:30", "10:00")))));

        verify(mensajeService, never()).enviarAviso(any(), any(), any());
    }
}
