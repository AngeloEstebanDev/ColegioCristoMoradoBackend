package com.CristoMorado_api.CristoMorado_api.pruebasunitarias;

import com.CristoMorado_api.CristoMorado_api.dto.CursoRequest;
import com.CristoMorado_api.CristoMorado_api.dto.CursoResponse;
import com.CristoMorado_api.CristoMorado_api.dto.SeccionRequest;
import com.CristoMorado_api.CristoMorado_api.dto.SeccionResponse;
import com.CristoMorado_api.CristoMorado_api.entity.Curso;
import com.CristoMorado_api.CristoMorado_api.entity.Seccion;
import com.CristoMorado_api.CristoMorado_api.exception.DatosInvalidosException;
import com.CristoMorado_api.CristoMorado_api.repository.*;
import com.CristoMorado_api.CristoMorado_api.services.CursoService;
import com.CristoMorado_api.CristoMorado_api.services.SeccionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** PU-07 – HU-08 y HU-09: Cursos y secciones (Épica 3, Sprint 3). CursoService.crear y SeccionService.crear. */
@DisplayName("PU-07 – HU-08 y HU-09: Cursos y secciones")
class PU07CursosSeccionesTest {

    private CursoRepository cursoRepository;
    private SeccionRepository seccionRepository;
    private CursoService cursos;
    private SeccionService secciones;

    @BeforeEach
    void setUp() {
        cursoRepository = mock(CursoRepository.class);
        seccionRepository = mock(SeccionRepository.class);
        SeccionCursoRepository seccionCursoRepository = mock(SeccionCursoRepository.class);
        cursos = new CursoService(cursoRepository, seccionCursoRepository);
        secciones = new SeccionService(seccionRepository, seccionCursoRepository, cursoRepository,
                mock(EstudianteRepository.class), mock(DocenteRepository.class));
        when(cursoRepository.save(any(Curso.class))).thenAnswer(inv -> { Curso c = inv.getArgument(0); c.setId(80L); return c; });
        when(seccionRepository.save(any(Seccion.class))).thenAnswer(inv -> { Seccion s = inv.getArgument(0); s.setId(20L); return s; });
    }

    @Test
    @DisplayName("PU-07.1 Datos obligatorios de un curso (nombre y categoría): validación exitosa")
    void c1_cursoValido() {
        CursoResponse c = cursos.crear(new CursoRequest("Robótica Educativa", "COMPUTACION"));

        assertEquals("Robótica Educativa", c.nombre());
        verify(cursoRepository, times(1)).save(any(Curso.class));
    }

    @Test
    @DisplayName("PU-07.2 Datos obligatorios de una sección (grado, letra y capacidad): validación exitosa")
    void c2_seccionValida() {
        SeccionResponse s = secciones.crear(new SeccionRequest("4° Primaria", "B", null, 30, null));

        assertEquals("4° Primaria", s.grado());
        assertEquals("B", s.letra());
        verify(seccionRepository, times(1)).save(any(Seccion.class));
    }

    @Test
    @DisplayName("PU-07.3 Datos incompletos (curso sin nombre o sin categoría, sección sin grado): se rechazan")
    void c3_datosIncompletos() {
        assertEquals("Ingresa el nombre del curso.",
                assertThrows(DatosInvalidosException.class, () -> cursos.crear(new CursoRequest("  ", "MATEMATICA"))).getMessage());
        assertEquals("Selecciona una categoría.",
                assertThrows(DatosInvalidosException.class, () -> cursos.crear(new CursoRequest("Robótica", null))).getMessage());
        assertEquals("Selecciona un grado válido.",
                assertThrows(DatosInvalidosException.class, () -> secciones.crear(new SeccionRequest(null, "A", null, 30, null))).getMessage());
        verify(cursoRepository, never()).save(any(Curso.class));
        verify(seccionRepository, never()).save(any(Seccion.class));
    }
}
