package com.CristoMorado_api.CristoMorado_api.pruebasunitarias;

import com.CristoMorado_api.CristoMorado_api.entity.*;
import com.CristoMorado_api.CristoMorado_api.exception.RecursoNoEncontradoException;
import com.CristoMorado_api.CristoMorado_api.repository.*;
import com.CristoMorado_api.CristoMorado_api.services.SeccionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** PU-08 – HU-10 y HU-12: Asignaciones académicas (Épica 3, Sprint 3). SeccionService.agregarCurso y matricular. */
@DisplayName("PU-08 – HU-10 y HU-12: Asignaciones académicas")
class PU08AsignacionesTest {

    private SeccionCursoRepository seccionCursoRepository;
    private EstudianteRepository estudianteRepository;
    private SeccionService servicio;
    private Estudiante lucia;

    @BeforeEach
    void setUp() {
        SeccionRepository seccionRepository = mock(SeccionRepository.class);
        seccionCursoRepository = mock(SeccionCursoRepository.class);
        CursoRepository cursoRepository = mock(CursoRepository.class);
        estudianteRepository = mock(EstudianteRepository.class);
        DocenteRepository docenteRepository = mock(DocenteRepository.class);
        servicio = new SeccionService(seccionRepository, seccionCursoRepository, cursoRepository, estudianteRepository, docenteRepository);
        Seccion quintoA = Seccion.builder().id(8L).grado("5° Primaria").letra("A").capacidad(30).estado("Activa").build();
        when(seccionRepository.findById(8L)).thenReturn(Optional.of(quintoA));
        when(cursoRepository.findById(80L)).thenReturn(Optional.of(Curso.builder().id(80L).nombre("Robótica Educativa").estado("Activo").build()));
        when(docenteRepository.findById(1L)).thenReturn(Optional.of(Docente.builder().id(1L).nombreCompleto("Carlos Mendoza Ruiz").estado("Activo").build()));
        lucia = Estudiante.builder().id(2L).nombreCompleto("Lucía Pérez García").dni("71234560").build();
        when(estudianteRepository.findById(2L)).thenReturn(Optional.of(lucia));
        when(estudianteRepository.countByGradoAndSeccion("5° Primaria", "A")).thenReturn(25L);
    }

    @Test
    @DisplayName("PU-08.1 Asignar un curso a un grado y sección existentes: la asignación es válida")
    void c1_asignarCurso() {
        servicio.agregarCurso(8L, 80L, 1L, "Taller de robótica");

        verify(seccionCursoRepository, times(1)).save(any(SeccionCurso.class));
    }

    @Test
    @DisplayName("PU-08.2 Asignar un estudiante a una sección existente: la operación es válida")
    void c2_matricularEstudiante() {
        servicio.matricular(8L, 2L);

        assertEquals("5° Primaria", lucia.getGrado());
        assertEquals("A", lucia.getSeccion());
        verify(estudianteRepository, times(1)).save(lucia);
    }

    @Test
    @DisplayName("PU-08.3 Identificador de sección inexistente: se rechaza la asignación")
    void c3_seccionInexistente() {
        assertThrows(RecursoNoEncontradoException.class, () -> servicio.agregarCurso(999L, 80L, 1L, null));
        assertThrows(RecursoNoEncontradoException.class, () -> servicio.matricular(999L, 2L));
        verify(seccionCursoRepository, never()).save(any(SeccionCurso.class));
        verify(estudianteRepository, never()).save(any(Estudiante.class));
    }
}
