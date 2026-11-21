package com.CristoMorado_api.CristoMorado_api.pruebasunitarias;

import com.CristoMorado_api.CristoMorado_api.dto.AulaDto;
import com.CristoMorado_api.CristoMorado_api.entity.*;
import com.CristoMorado_api.CristoMorado_api.exception.DatosInvalidosException;
import com.CristoMorado_api.CristoMorado_api.repository.*;
import com.CristoMorado_api.CristoMorado_api.services.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** PU-12 – HU-14: Gestión de actividades (Épica 4, Sprint 4). AulaService.crearActividad (docente). */
@DisplayName("PU-12 – HU-14: Gestión de actividades")
class PU12ActividadesTest {

    private static final Long DOCENTE = 7L, CURSO = 64L;

    private ActividadRepository actividadRepository;
    private AulaService servicio;

    /**
     * Se ejecuta antes de cada prueba: crea los mocks (objetos simulados con Mockito) y los datos de
     * ejemplo. Guarda los cambios en la base de datos.
     */
    @BeforeEach
    void setUp() {
        actividadRepository = mock(ActividadRepository.class);
        AccesoService accesoService = mock(AccesoService.class);
        servicio = new AulaService(mock(MaterialRepository.class), actividadRepository, mock(EntregaRepository.class),
                mock(SeccionCursoRepository.class), mock(SeccionRepository.class), mock(EstudianteRepository.class),
                mock(UsuarioRepository.class), mock(EvaluacionRepository.class), accesoService,
                mock(ArchivoService.class), mock(NotaService.class), mock(MensajeService.class), mock(CarpetaRepository.class));
        Seccion quintoA = Seccion.builder().id(8L).grado("5° Primaria").letra("A").build();
        SeccionCurso matematica = SeccionCurso.builder().id(CURSO).seccion(quintoA)
                .curso(Curso.builder().id(1L).nombre("Matemática").color("#2563EB").build()).estado("Activo").build();
        when(accesoService.cursoDelDocente(DOCENTE, CURSO)).thenReturn(matematica);
        when(accesoService.cuentasAlumnos(quintoA)).thenReturn(List.of());
        when(actividadRepository.save(any(Actividad.class))).thenAnswer(inv -> { Actividad a = inv.getArgument(0); a.setId(501L); return a; });
    }

    /**
     * Devuelve actividad.
     */
    private static AulaDto.ActividadRequest actividad(String titulo, LocalDateTime limite) {
        return new AulaDto.ActividadRequest("TAREA", titulo, "<p>Resuelve la página 45 del libro.</p>", limite, 20.0,
                false, null, null, null, List.of(), 1, null, List.of("PDF"));
    }

    /**
     * Caso de prueba: PU-12.1 Actividad con los campos obligatorios completos: validación exitosa.
     * Prepara los datos (arrange), ejecuta el método (act) y comprueba el resultado esperado (assert).
     * Guarda los cambios en la base de datos.
     */
    @Test
    @DisplayName("PU-12.1 Actividad con los campos obligatorios completos: validación exitosa")
    void c1_actividadValida() {
        AulaDto.ActividadResponse r = servicio.crearActividad(DOCENTE, CURSO,
                actividad("Fracciones equivalentes", LocalDateTime.now().plusDays(3)));

        assertEquals("Fracciones equivalentes", r.titulo());
        verify(actividadRepository, times(1)).save(any(Actividad.class));
    }

    /**
     * Caso de prueba: PU-12.2 Actividad sin título: se rechaza. Prepara los datos (arrange), ejecuta
     * el método (act) y comprueba el resultado esperado (assert). Guarda los cambios en la base de
     * datos.
     */
    @Test
    @DisplayName("PU-12.2 Actividad sin título: se rechaza")
    void c2_sinTitulo() {
        DatosInvalidosException ex = assertThrows(DatosInvalidosException.class,
                () -> servicio.crearActividad(DOCENTE, CURSO, actividad("   ", LocalDateTime.now().plusDays(3))));

        assertEquals("Escribe el título.", ex.getMessage());
        verify(actividadRepository, never()).save(any(Actividad.class));
    }

    /**
     * Caso de prueba: PU-12.3 Fecha de entrega inválida (vacía o ya pasada): se rechaza. Prepara los
     * datos (arrange), ejecuta el método (act) y comprueba el resultado esperado (assert). Guarda los
     * cambios en la base de datos.
     */
    @Test
    @DisplayName("PU-12.3 Fecha de entrega inválida (vacía o ya pasada): se rechaza")
    void c3_fechaInvalida() {
        assertEquals("Indica la fecha y hora límite.", assertThrows(DatosInvalidosException.class,
                () -> servicio.crearActividad(DOCENTE, CURSO, actividad("Fracciones", null))).getMessage());
        assertEquals("La fecha límite debe ser posterior a este momento.", assertThrows(DatosInvalidosException.class,
                () -> servicio.crearActividad(DOCENTE, CURSO, actividad("Fracciones", LocalDateTime.now().minusDays(1)))).getMessage());
        verify(actividadRepository, never()).save(any(Actividad.class));
    }
}
