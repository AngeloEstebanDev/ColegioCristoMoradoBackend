package com.CristoMorado_api.CristoMorado_api.pruebasunitarias;

import com.CristoMorado_api.CristoMorado_api.dto.ComunicadoRequest;
import com.CristoMorado_api.CristoMorado_api.dto.ComunicadoResponse;
import com.CristoMorado_api.CristoMorado_api.entity.Comunicado;
import com.CristoMorado_api.CristoMorado_api.entity.Usuario;
import com.CristoMorado_api.CristoMorado_api.exception.DatosInvalidosException;
import com.CristoMorado_api.CristoMorado_api.repository.*;
import com.CristoMorado_api.CristoMorado_api.services.AccesoService;
import com.CristoMorado_api.CristoMorado_api.services.ComunicadoService;
import com.CristoMorado_api.CristoMorado_api.services.PermisoService;
import com.CristoMorado_api.CristoMorado_api.util.Permiso;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * PU-14 – HU-24 y HU-25: Comunicados y notificaciones (Épica 4, Sprint 4).
 * ComunicadoService.crear valida el comunicado; ComunicadoService.listar decide a quién le llega (destinatarios).
 */
@DisplayName("PU-14 – HU-24 y HU-25: Comunicados y notificaciones")
class PU14ComunicadosTest {

    private static final String MENSAJE = "<p>Reunión de padres de familia el viernes a las 6 p. m. en el auditorio.</p>";

    private ComunicadoRepository comunicadoRepository;
    private UsuarioRepository usuarioRepository;
    private ComunicadoService servicio;

    @BeforeEach
    void setUp() {
        comunicadoRepository = mock(ComunicadoRepository.class);
        usuarioRepository = mock(UsuarioRepository.class);
        PermisoService permisoService = mock(PermisoService.class);
        servicio = new ComunicadoService(comunicadoRepository, mock(ComunicadoLecturaRepository.class), usuarioRepository,
                permisoService, mock(AccesoService.class), mock(SeccionRepository.class),
                mock(EstudianteRepository.class), mock(PadreFamiliaRepository.class));
        Usuario admin = Usuario.builder().id(1L).nombre("Paola Morales Guido").usuario("admin123").rol("ADMIN").estado(true).build();
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(usuarioRepository.findById(30L)).thenReturn(Optional.of(
                Usuario.builder().id(30L).nombre("Juan Pérez Rojas").usuario("APO76543210").rol("PADRE").estado(true).build()));
        when(usuarioRepository.findById(32L)).thenReturn(Optional.of(
                Usuario.builder().id(32L).nombre("Juan Pérez García").usuario("ALU87654321").rol("ALUMNO").estado(true).build()));
        when(permisoService.tiene(admin, Permiso.COMUNICADOS)).thenReturn(true);
        when(comunicadoRepository.save(any(Comunicado.class))).thenAnswer(inv -> { Comunicado c = inv.getArgument(0); c.setId(100L); return c; });
        when(comunicadoRepository.existsById(100L)).thenReturn(true);
    }

    private static ComunicadoRequest comunicado(String titulo, String contenido, List<String> grupos) {
        return new ComunicadoRequest(titulo, contenido, null, false, grupos, null);
    }

    @Test
    @DisplayName("PU-14.1 Comunicado con los campos obligatorios completos: es válido y se publica")
    void c1_comunicadoValido() {
        ComunicadoResponse r = servicio.crear(1L, comunicado("Reunión de Padres de Familia", MENSAJE, List.of("PADRES")));

        assertEquals("Reunión de Padres de Familia", r.titulo());
        assertEquals(List.of("PADRES"), r.destinatarios());
        verify(comunicadoRepository, times(1)).save(any(Comunicado.class));
    }

    @Test
    @DisplayName("PU-14.2 Comunicado sin título o sin mensaje: se rechaza")
    void c2_sinTituloOMensaje() {
        assertEquals("El título es obligatorio.", assertThrows(DatosInvalidosException.class,
                () -> servicio.crear(1L, comunicado("  ", MENSAJE, List.of("PADRES")))).getMessage());
        assertThrows(DatosInvalidosException.class, () -> servicio.crear(1L, comunicado("Reunión de Padres", "<p></p>", List.of("PADRES"))));
        verify(comunicadoRepository, never()).save(any(Comunicado.class));
    }

    @Test
    @DisplayName("PU-14.3 Destinatarios válidos (PADRES): le llega al padre y no al alumno")
    void c3_identificaDestinatarios() {
        Comunicado c = Comunicado.builder().id(100L).titulo("Reunión de Padres de Familia").contenido(MENSAJE)
                .destinatarios(new LinkedHashSet<>(Set.of("PADRES"))).secciones(new LinkedHashSet<>())
                .autor(Usuario.builder().id(1L).nombre("Paola Morales Guido").rol("ADMIN").build()).build();
        when(comunicadoRepository.findAllByOrderByFechaCreacionDesc()).thenReturn(List.of(c));

        List<ComunicadoResponse> padre = servicio.listar(30L);
        List<ComunicadoResponse> alumno = servicio.listar(32L);

        assertEquals(1, padre.size());
        assertEquals("Reunión de Padres de Familia", padre.get(0).titulo());
        assertEquals(0, alumno.size());
    }
}
