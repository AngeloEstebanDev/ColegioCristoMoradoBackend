package com.CristoMorado_api.CristoMorado_api.pruebasunitarias;

import com.CristoMorado_api.CristoMorado_api.dto.VinculoRequest;
import com.CristoMorado_api.CristoMorado_api.dto.VinculoResponse;
import com.CristoMorado_api.CristoMorado_api.entity.Estudiante;
import com.CristoMorado_api.CristoMorado_api.entity.EstudianteFamiliar;
import com.CristoMorado_api.CristoMorado_api.entity.PadreFamilia;
import com.CristoMorado_api.CristoMorado_api.exception.RecursoNoEncontradoException;
import com.CristoMorado_api.CristoMorado_api.repository.EstudianteFamiliarRepository;
import com.CristoMorado_api.CristoMorado_api.repository.EstudianteRepository;
import com.CristoMorado_api.CristoMorado_api.repository.PadreFamiliaRepository;
import com.CristoMorado_api.CristoMorado_api.services.VinculoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** PU-06 – HU-11: Asociación padre-estudiante (Épica 2, Sprint 2). VinculoService.crear. */
@DisplayName("PU-06 – HU-11: Asociación padre-estudiante")
class PU06AsociacionPadreTest {

    private EstudianteFamiliarRepository vinculoRepository;
    private VinculoService servicio;

    @BeforeEach
    void setUp() {
        vinculoRepository = mock(EstudianteFamiliarRepository.class);
        EstudianteRepository estudianteRepository = mock(EstudianteRepository.class);
        PadreFamiliaRepository padreRepository = mock(PadreFamiliaRepository.class);
        servicio = new VinculoService(vinculoRepository, estudianteRepository, padreRepository);
        when(padreRepository.findById(1L)).thenReturn(Optional.of(
                PadreFamilia.builder().id(1L).nombreCompleto("Juan Pérez Rojas").dni("76543210").build()));
        when(estudianteRepository.findById(1L)).thenReturn(Optional.of(
                Estudiante.builder().id(1L).nombreCompleto("Juan Pérez García").dni("87654321").build()));
        when(vinculoRepository.save(any(EstudianteFamiliar.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("PU-06.1 Padre y estudiante existentes: la asociación es válida")
    void c1_asociacionValida() {
        VinculoResponse r = servicio.crear(new VinculoRequest(1L, 1L, "Padre"));

        assertEquals("Padre", r.parentesco());
        verify(vinculoRepository, times(1)).save(any(EstudianteFamiliar.class));
    }

    @Test
    @DisplayName("PU-06.2 Identificador de padre inexistente: se rechaza la operación")
    void c2_padreInexistente() {
        RecursoNoEncontradoException ex = assertThrows(RecursoNoEncontradoException.class,
                () -> servicio.crear(new VinculoRequest(999L, 1L, "Padre")));

        assertEquals("Padre de familia no encontrado.", ex.getMessage());
        verify(vinculoRepository, never()).save(any(EstudianteFamiliar.class));
    }

    @Test
    @DisplayName("PU-06.3 Identificador de estudiante inexistente: se rechaza la operación")
    void c3_estudianteInexistente() {
        RecursoNoEncontradoException ex = assertThrows(RecursoNoEncontradoException.class,
                () -> servicio.crear(new VinculoRequest(1L, 999L, "Padre")));

        assertEquals("Alumno no encontrado.", ex.getMessage());
        verify(vinculoRepository, never()).save(any(EstudianteFamiliar.class));
    }
}
