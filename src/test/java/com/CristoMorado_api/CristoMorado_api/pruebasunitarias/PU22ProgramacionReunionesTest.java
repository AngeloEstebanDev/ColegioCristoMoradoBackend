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

/** PU-22 – HU-28: Programación de reuniones (Épica 6). AgendaService.programar (la administración cita a un padre). */
@DisplayName("PU-22 – HU-28: Programación de reuniones")
class PU22ProgramacionReunionesTest {

    private static final Long ADMIN = 1L, PADRE = 30L;

    private AgendaService servicio;
    private UsuarioRepository usuarioRepository;
    private CitaRepository citaRepository;
    private DisponibilidadRepository disponibilidadRepository;
    private MensajeService mensajeService;
    private Usuario admin, uPadre;
    private EstudianteRepository estudianteRepository;

    /** Administración (usuario 1) atiende a padres; Luis Pérez (usuario 30) es padre de Juan (5° Primaria A). */
    @BeforeEach
    void setUp() {
        usuarioRepository = mock(UsuarioRepository.class);
        PadreFamiliaRepository padreRepository = mock(PadreFamiliaRepository.class);
        citaRepository = mock(CitaRepository.class);
        disponibilidadRepository = mock(DisponibilidadRepository.class);
        mensajeService = mock(MensajeService.class);
        estudianteRepository = mock(EstudianteRepository.class);
        AccesoService acceso = new AccesoService(usuarioRepository, mock(DocenteRepository.class), mock(EstudianteRepository.class),
                padreRepository, mock(SeccionRepository.class), mock(SeccionCursoRepository.class));
        servicio = new AgendaService(disponibilidadRepository, citaRepository, usuarioRepository, padreRepository,
                estudianteRepository, mock(PersonalRepository.class), mock(SeccionCursoRepository.class),
                mock(SeccionRepository.class), acceso, mensajeService);

        admin = Usuario.builder().id(ADMIN).nombre("Secretaría Académica").rol("ADMIN").build();
        uPadre = Usuario.builder().id(PADRE).nombre("Luis Pérez Castro").rol("PADRE").build();
        PadreFamilia padre = PadreFamilia.builder().id(3L).nombreCompleto("Luis Pérez Castro").telefono("987654321").usuario(uPadre).build();
        Estudiante juan = Estudiante.builder().id(1L).nombreCompleto("Juan Pérez García").grado("5° Primaria").seccion("A").build();
        padre.getVinculosFamiliares().add(EstudianteFamiliar.builder().id(1L).padreFamilia(padre).estudiante(juan).parentesco("Padre").build());

        when(estudianteRepository.findById(1L)).thenReturn(Optional.of(juan));
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

    private static CitaDto.ProgramarRequest pedido(LocalDate fecha, String hora) {
        return new CitaDto.ProgramarRequest(PADRE, 1L, fecha, hora, null, "PRESENCIAL", "Dirección",
                "Rendimiento en Matemática", 30, null);
    }

    @Test
    @DisplayName("PU-22.1 Fecha, hora y responsables válidos: la reunión se programa")
    void c1_programacionExitosa() {
        LocalDate fecha = LocalDate.now().plusDays(7);
        CitaDto.Cita c = servicio.programar(ADMIN, pedido(fecha, "10:00"));

        assertEquals(fecha, c.fecha());
        assertEquals("10:00", c.horaInicio());
        assertEquals("10:30", c.horaFin());
        assertEquals(Cita.PENDIENTE, c.estado());
        assertEquals("Juan Pérez García", c.estudiante().nombre());
        verify(citaRepository, times(1)).save(any(Cita.class));
        verify(mensajeService).enviarAviso(any(), eq("Nueva cita"), anyString());
    }

    @Test
    @DisplayName("PU-22.2 Fecha u hora inválida (pasada, vacía o mal escrita): se rechaza")
    void c2_fechaHoraInvalida() {
        assertEquals("La fecha y hora ya pasaron.", assertThrows(DatosInvalidosException.class,
                () -> servicio.programar(ADMIN, pedido(LocalDate.now().minusDays(1), "10:00"))).getMessage());
        assertEquals("Indica la fecha de la reunión.", assertThrows(DatosInvalidosException.class,
                () -> servicio.programar(ADMIN, pedido(null, "10:00"))).getMessage());
        assertEquals("Hora no válida: 25:99 (usa HH:mm).", assertThrows(DatosInvalidosException.class,
                () -> servicio.programar(ADMIN, pedido(LocalDate.now().plusDays(7), "25:99"))).getMessage());
        verify(citaRepository, never()).save(any(Cita.class));
    }

    @Test
    @DisplayName("PU-22.3 Horario ya ocupado por otra reunión: se rechaza")
    void c3_horarioOcupado() {
        LocalDate fecha = LocalDate.now().plusDays(7);
        when(citaRepository.findByResponsableIdAndFechaAndEstadoIn(any(), any(), any()))
                .thenReturn(List.of(cita(51L, fecha, 10, 0, Cita.CONFIRMADA)));

        assertEquals("Ya tienes una reunión de 10:00 a 10:30 ese día.", assertThrows(DatosInvalidosException.class,
                () -> servicio.programar(ADMIN, pedido(fecha, "10:00"))).getMessage());
        verify(citaRepository, never()).save(any(Cita.class));
    }
}
