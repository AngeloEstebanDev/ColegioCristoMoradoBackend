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

/** PU-23 – HU-29: Reprogramación y cancelación de reuniones (Épica 6). AgendaService.reprogramar y cancelar. */
@DisplayName("PU-23 – HU-29: Reprogramación y cancelación")
class PU23ReprogramacionCancelacionTest {

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
    @DisplayName("PU-23.1 Reprogramación válida: se actualizan la fecha y la hora")
    void c1_reprogramacionValida() {
        Cita c = cita(50L, LocalDate.now().plusDays(7), 15, 0, Cita.CONFIRMADA);
        when(citaRepository.findById(50L)).thenReturn(Optional.of(c));
        LocalDate nueva = LocalDate.now().plusDays(8);

        servicio.reprogramar(ADMIN, 50L, new CitaDto.ReprogramarRequest(nueva, "11:00", null, "Cruce con la actuación escolar"));

        assertEquals(nueva, c.getFecha());
        assertEquals(LocalTime.of(11, 0), c.getHoraInicio());
        assertEquals(LocalTime.of(11, 30), c.getHoraFin());
        assertEquals(Cita.CONFIRMADA, c.getEstado());
        assertEquals(1, c.getReprogramaciones().intValue());
        verify(citaRepository).save(c);
    }

    @Test
    @DisplayName("PU-23.2 Cancelación de una reunión: el estado cambia a «Cancelado»")
    void c2_cancelacion() {
        Cita c = cita(50L, LocalDate.now().plusDays(7), 15, 0, Cita.CONFIRMADA);
        when(citaRepository.findById(50L)).thenReturn(Optional.of(c));

        CitaDto.Cita r = servicio.cancelar(PADRE, 50L, "Viaje de trabajo");

        assertEquals(Cita.CANCELADA, c.getEstado());
        assertEquals(Cita.CANCELADA, r.estado());
        assertEquals("Viaje de trabajo", c.getObservacion());
        verify(mensajeService).enviarAviso(any(), eq("Reunión cancelada"), anyString());
    }

    @Test
    @DisplayName("PU-23.3 La cancelación libera el horario para otra reunión")
    @SuppressWarnings("unchecked")
    void c3_cancelacionLiberaHorario() {
        LocalDate fecha = LocalDate.now().plusDays(7);
        Cita c = cita(50L, fecha, 15, 0, Cita.CONFIRMADA);
        when(citaRepository.findById(50L)).thenReturn(Optional.of(c));
        // El repositorio solo devuelve las citas cuyo estado esté en la lista pedida (PENDIENTE / CONFIRMADA).
        when(citaRepository.findByResponsableIdAndFechaBetweenAndEstadoIn(any(), any(), any(), any())).thenAnswer(i ->
                ((Collection<String>) i.getArgument(3)).contains(c.getEstado()) ? List.of(c) : List.of());

        assertFalse(tiene(servicio.horariosLibres(PADRE, ADMIN), fecha, "15:00"), "Antes: 15:00 ocupado");
        servicio.cancelar(PADRE, 50L, "Viaje de trabajo");
        assertTrue(tiene(servicio.horariosLibres(PADRE, ADMIN), fecha, "15:00"), "Después: 15:00 libre");
    }
}
