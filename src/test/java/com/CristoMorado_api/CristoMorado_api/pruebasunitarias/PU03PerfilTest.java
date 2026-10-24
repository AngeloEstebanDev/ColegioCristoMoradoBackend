package com.CristoMorado_api.CristoMorado_api.pruebasunitarias;

import com.CristoMorado_api.CristoMorado_api.dto.PerfilUpdateRequest;
import com.CristoMorado_api.CristoMorado_api.entity.Docente;
import com.CristoMorado_api.CristoMorado_api.entity.Usuario;
import com.CristoMorado_api.CristoMorado_api.exception.DatosInvalidosException;
import com.CristoMorado_api.CristoMorado_api.repository.*;
import com.CristoMorado_api.CristoMorado_api.services.PerfilService;
import com.CristoMorado_api.CristoMorado_api.services.PermisoService;
import com.CristoMorado_api.CristoMorado_api.services.VerificacionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * PU-03 – HU-32: Gestión del perfil (Épica 1, Sprint 1).
 * PerfilService.actualizar valida los datos de "Mi perfil" antes de guardarlos.
 */
@DisplayName("PU-03 – HU-32: Gestión del perfil")
class PU03PerfilTest {

    private UsuarioRepository usuarioRepository;
    private DocenteRepository docenteRepository;
    private PerfilService servicio;
    private Docente carlos;

    @BeforeEach
    void setUp() {
        usuarioRepository = mock(UsuarioRepository.class);
        docenteRepository = mock(DocenteRepository.class);
        PadreFamiliaRepository padreRepository = mock(PadreFamiliaRepository.class);
        PersonalRepository personalRepository = mock(PersonalRepository.class);
        servicio = new PerfilService(usuarioRepository, docenteRepository, mock(EstudianteRepository.class),
                padreRepository, mock(VerificacionService.class), personalRepository, mock(PermisoService.class));
        Usuario cuenta = Usuario.builder().id(7L).usuario("DOC45678912").nombre("Carlos Mendoza Ruiz")
                .rol("DOCENTE").estado(true).build();
        carlos = Docente.builder().id(1L).nombreCompleto("Carlos Mendoza Ruiz").dni("45678912").usuarioId(7L).build();
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(cuenta));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));
        when(docenteRepository.findByUsuarioId(7L)).thenReturn(Optional.of(carlos));
        when(docenteRepository.save(any(Docente.class))).thenAnswer(inv -> inv.getArgument(0));
        when(padreRepository.findByUsuarioId(7L)).thenReturn(Optional.empty());
        when(personalRepository.findByUsuarioId(7L)).thenReturn(Optional.empty());
    }

    private static PerfilUpdateRequest perfil(String nombre, String telefono, String correo) {
        return new PerfilUpdateRequest(nombre, null, null, null, telefono, "Av. Los Olivos 123", null, null, correo,
                null, null, null, null, null);
    }

    @Test
    @DisplayName("PU-03.1 Datos válidos: la validación los acepta y se guardan")
    void c1_datosValidos() {
        servicio.actualizar(7L, perfil("Carlos Mendoza Ruiz", "987 654 321", "carlos.mendoza@colegio.edu.pe"));

        assertEquals("987654321", carlos.getTelefono(), "El teléfono se guarda solo con sus 9 dígitos");
        assertEquals("carlos.mendoza@colegio.edu.pe", carlos.getCorreo());
        verify(docenteRepository, times(1)).save(carlos);
    }

    @Test
    @DisplayName("PU-03.2 Correo con formato incorrecto: se rechaza")
    void c2_correoIncorrecto() {
        DatosInvalidosException ex = assertThrows(DatosInvalidosException.class,
                () -> servicio.actualizar(7L, perfil("Carlos Mendoza Ruiz", "987654321", "carlos.mendoza#colegio")));

        assertEquals("El correo de contacto no es válido.", ex.getMessage());
        verify(docenteRepository, never()).save(any(Docente.class));
    }

    @Test
    @DisplayName("PU-03.3 Campo obligatorio vacío (nombre): error de validación")
    void c3_campoObligatorioVacio() {
        DatosInvalidosException ex = assertThrows(DatosInvalidosException.class,
                () -> servicio.actualizar(7L, perfil("   ", "987654321", null)));

        assertEquals("Escribe tu nombre completo.", ex.getMessage());
        verify(docenteRepository, never()).save(any(Docente.class));
    }
}
