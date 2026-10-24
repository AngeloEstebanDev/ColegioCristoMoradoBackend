package com.CristoMorado_api.CristoMorado_api.pruebasunitarias;

import com.CristoMorado_api.CristoMorado_api.dto.NotaDto;
import com.CristoMorado_api.CristoMorado_api.entity.*;
import com.CristoMorado_api.CristoMorado_api.exception.AccesoDenegadoException;
import com.CristoMorado_api.CristoMorado_api.exception.RecursoNoEncontradoException;
import com.CristoMorado_api.CristoMorado_api.repository.*;
import com.CristoMorado_api.CristoMorado_api.services.AccesoService;
import com.CristoMorado_api.CristoMorado_api.services.NotaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * PU-17 – HU-20: Consulta de calificaciones (Épica 5, Sprint 5).
 * NotaService.boleta + AccesoService.estudianteVisible (servicio real con repositorios simulados).
 */
@DisplayName("PU-17 – HU-20: Consulta de calificaciones")
class PU17ConsultaCalificacionesTest {

    private static final Long JUAN_USUARIO = 32L, MARIA_USUARIO = 33L;
    private static final int ANIO = LocalDate.now().getYear();

    private NotaService servicio;
    private EstudianteRepository estudianteRepository;
    private NotaRepository notaRepository;

    @BeforeEach
    void setUp() {
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
        estudianteRepository = mock(EstudianteRepository.class);
        notaRepository = mock(NotaRepository.class);
        EvaluacionRepository evaluacionRepository = mock(EvaluacionRepository.class);
        PeriodoRepository periodoRepository = mock(PeriodoRepository.class);
        AccesoService acceso = new AccesoService(usuarioRepository, mock(DocenteRepository.class), estudianteRepository,
                mock(PadreFamiliaRepository.class), mock(SeccionRepository.class), mock(SeccionCursoRepository.class));
        servicio = new NotaService(evaluacionRepository, notaRepository, mock(NotaHistorialRepository.class), periodoRepository,
                estudianteRepository, mock(ActividadRepository.class), mock(SeccionCursoRepository.class),
                mock(AsistenciaRepository.class), acceso, mock(EntregaRepository.class));

        Usuario uJuan = Usuario.builder().id(JUAN_USUARIO).nombre("Juan Pérez García").rol("ALUMNO").build();
        Usuario uMaria = Usuario.builder().id(MARIA_USUARIO).nombre("María López Ríos").rol("ALUMNO").build();
        when(usuarioRepository.findById(JUAN_USUARIO)).thenReturn(Optional.of(uJuan));
        when(usuarioRepository.findById(MARIA_USUARIO)).thenReturn(Optional.of(uMaria));

        Estudiante juan = Estudiante.builder().id(1L).nombreCompleto("Juan Pérez García").grado("5° Primaria").seccion("A").usuario(uJuan).build();
        Estudiante maria = Estudiante.builder().id(2L).nombreCompleto("María López Ríos").grado("5° Primaria").seccion("A").usuario(uMaria).build();
        when(estudianteRepository.findById(1L)).thenReturn(Optional.of(juan));
        when(estudianteRepository.findById(2L)).thenReturn(Optional.of(maria));

        Periodo bim1 = Periodo.builder().id(5L).nombre("I Bimestre " + ANIO).anio(ANIO).bimestre(1).build();
        Seccion quintoA = Seccion.builder().id(8L).grado("5° Primaria").letra("A").build();
        SeccionCurso matematica = SeccionCurso.builder().id(64L).seccion(quintoA)
                .curso(Curso.builder().id(1L).nombre("Matemática").categoria("MATEMATICA").build()).build();
        Evaluacion examen = Evaluacion.builder().id(20L).seccionCurso(matematica).periodo(bim1).nombre("Examen bimestral")
                .tipo("EXAMEN").peso(1).build();
        when(periodoRepository.findAll()).thenReturn(List.of(bim1));
        when(notaRepository.findByEstudianteId(1L)).thenReturn(List.of(
                Nota.builder().id(40L).evaluacion(examen).estudiante(juan).valor(16.0).build()));
        when(evaluacionRepository.findBySeccionCursoIdAndPeriodoIdOrderByFechaRegistroAsc(64L, 5L)).thenReturn(List.of(examen));
    }

    @Test
    @DisplayName("PU-17.1 Estudiante existente: se obtienen sus calificaciones (Matemática 16)")
    void c1_estudianteExistente() {
        NotaDto.Boleta b = servicio.boleta(JUAN_USUARIO, 1L, ANIO);

        assertEquals("Juan Pérez García", b.nombre());
        assertEquals(1, b.cursos().size());
        assertEquals("Matemática", b.cursos().get(0).curso());
        assertEquals(16.0, b.cursos().get(0).periodos().get(0).notas().get(0).valor(), 0.001);
        assertEquals(1L, b.cursosAprobados());
    }

    @Test
    @DisplayName("PU-17.2 Estudiante inexistente: se maneja correctamente (mensaje «Alumno no encontrado.»)")
    void c2_estudianteInexistente() {
        RecursoNoEncontradoException ex = assertThrows(RecursoNoEncontradoException.class,
                () -> servicio.boleta(JUAN_USUARIO, 999L, ANIO));
        assertEquals("Alumno no encontrado.", ex.getMessage());
        verify(notaRepository, never()).findByEstudianteId(999L);
    }

    @Test
    @DisplayName("PU-17.3 Autorización: un alumno no puede ver las notas de otro alumno")
    void c3_autorizacionRechazaDatosNoPermitidos() {
        AccesoDenegadoException ex = assertThrows(AccesoDenegadoException.class,
                () -> servicio.boleta(MARIA_USUARIO, 1L, ANIO));
        assertEquals("No tienes acceso a la información de este alumno.", ex.getMessage());
        verify(notaRepository, never()).findByEstudianteId(1L);
    }
}
