package com.CristoMorado_api.CristoMorado_api.pruebasunitarias;

import com.CristoMorado_api.CristoMorado_api.dto.CitaDto;
import com.CristoMorado_api.CristoMorado_api.entity.*;
import com.CristoMorado_api.CristoMorado_api.exception.AccesoDenegadoException;
import com.CristoMorado_api.CristoMorado_api.exception.DatosInvalidosException;
import com.CristoMorado_api.CristoMorado_api.repository.*;
import com.CristoMorado_api.CristoMorado_api.services.AccesoService;
import com.CristoMorado_api.CristoMorado_api.services.AgendaService;
import com.CristoMorado_api.CristoMorado_api.services.MensajeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** PU-20 – HU-21: Consulta de citas (Épica 6, Sprint 6). AgendaService.misCitas. */
@DisplayName("PU-20 – HU-21: Consulta de citas")
class PU20CitasTest {

    private static final Long ADMIN = 1L, PADRE = 30L;

    private AgendaService servicio;
    private UsuarioRepository usuarioRepository;
    private CitaRepository citaRepository;
    private DisponibilidadRepository disponibilidadRepository;
    private MensajeService mensajeService;
    private Usuario admin, uPadre;

    /** Administración (usuario 1) atiende a padres; Luis Pérez (usuario 30) es padre de Juan (5° Primaria A). */
    @BeforeEach
    void setUp() {
        usuarioRepository = mock(UsuarioRepository.class);
        PadreFamiliaRepository padreRepository = mock(PadreFamiliaRepository.class);
        citaRepository = mock(CitaRepository.class);
        disponibilidadRepository = mock(DisponibilidadRepository.class);
        mensajeService = mock(MensajeService.class);
        AccesoService acceso = new AccesoService(usuarioRepository, mock(DocenteRepository.class), mock(EstudianteRepository.class),
                padreRepository, mock(SeccionRepository.class), mock(SeccionCursoRepository.class));
        servicio = new AgendaService(disponibilidadRepository, citaRepository, usuarioRepository, padreRepository,
                mock(EstudianteRepository.class), mock(PersonalRepository.class), mock(SeccionCursoRepository.class),
                mock(SeccionRepository.class), acceso, mensajeService);

        admin = Usuario.builder().id(ADMIN).nombre("Secretaría Académica").rol("ADMIN").build();
        uPadre = Usuario.builder().id(PADRE).nombre("Luis Pérez Castro").rol("PADRE").build();
        PadreFamilia padre = PadreFamilia.builder().id(3L).nombreCompleto("Luis Pérez Castro").telefono("987654321").usuario(uPadre).build();
        Estudiante juan = Estudiante.builder().id(1L).nombreCompleto("Juan Pérez García").grado("5° Primaria").seccion("A").build();
        padre.getVinculosFamiliares().add(EstudianteFamiliar.builder().id(1L).padreFamilia(padre).estudiante(juan).parentesco("Padre").build());

        when(usuarioRepository.findById(ADMIN)).thenReturn(Optional.of(admin));
        when(usuarioRepository.findById(PADRE)).thenReturn(Optional.of(uPadre));
        when(padreRepository.findByUsuarioId(PADRE)).thenReturn(Optional.of(padre));
        when(padreRepository.findAll()).thenReturn(List.of(padre));
        when(citaRepository.save(any(Cita.class))).thenAnswer(i -> i.getArgument(0));
        // Disponibilidad de la administración: todos los días de 15:00 a 16:00, citas de 30 minutos.
        List<Disponibilidad> horario = new ArrayList<>();
        for (int dia = 1; dia <= 7; dia++) {
            horario.add(Disponibilidad.builder().id((long) dia).usuario(admin).dia(dia).horaInicio(LocalTime.of(15, 0))
                    .horaFin(LocalTime.of(16, 0)).duracionMinutos(30).modalidad("PRESENCIAL").lugar("Dirección").activo(true).build());
        }
        when(disponibilidadRepository.findByUsuarioIdAndActivoTrue(ADMIN)).thenReturn(horario);
    }

    private Cita cita(Long id, LocalDate fecha, int hora, int minuto, String estado) {
        LocalTime ini = LocalTime.of(hora, minuto);
        return Cita.builder().id(id).responsable(admin).padre(uPadre).fecha(fecha).horaInicio(ini).horaFin(ini.plusMinutes(30))
                .modalidad("PRESENCIAL").motivo("Rendimiento en Matemática").origen(Cita.PROGRAMADA).estado(estado)
                .reprogramaciones(0).build();
    }

    private static boolean tiene(List<CitaDto.Horario> libres, LocalDate fecha, String hora) {
        return libres.stream().anyMatch(h -> h.fecha().equals(fecha) && h.horaInicio().equals(hora));
    }

    @Test
    @DisplayName("PU-20.1 Usuario con citas registradas: se devuelven sus citas")
    void c1_usuarioConCitas() {
        LocalDate fecha = LocalDate.now().plusDays(7);
        when(citaRepository.findByPadreIdOrderByFechaDescHoraInicioDesc(PADRE)).thenReturn(List.of(
                cita(50L, fecha, 15, 30, Cita.PENDIENTE), cita(51L, fecha, 15, 0, Cita.CONFIRMADA)));

        List<CitaDto.Cita> citas = servicio.misCitas(PADRE);

        assertEquals(2, citas.size());
        assertEquals(50L, citas.get(0).id());
        assertEquals("15:30", citas.get(0).horaInicio());
        assertEquals("Secretaría Académica", citas.get(0).responsable().nombre());
        assertEquals("PADRE", citas.get(0).miRol());
    }

    @Test
    @DisplayName("PU-20.2 Usuario sin citas: se devuelve una lista vacía")
    void c2_usuarioSinCitas() {
        List<CitaDto.Cita> citas = assertDoesNotThrow(() -> servicio.misCitas(PADRE));
        assertNotNull(citas);
        assertTrue(citas.isEmpty());
    }

    @Test
    @DisplayName("PU-20.3 Identificador de usuario inválido (999 o vacío): el error se maneja correctamente")
    void c3_idInvalido() {
        assertEquals("Usuario no válido.", assertThrows(AccesoDenegadoException.class, () -> servicio.misCitas(999L)).getMessage());
        assertEquals("Inicia sesión para continuar.", assertThrows(AccesoDenegadoException.class, () -> servicio.misCitas(null)).getMessage());
        verifyNoInteractions(citaRepository);
    }
}
