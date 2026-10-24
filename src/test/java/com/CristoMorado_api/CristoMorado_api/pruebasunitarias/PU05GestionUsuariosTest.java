package com.CristoMorado_api.CristoMorado_api.pruebasunitarias;

import com.CristoMorado_api.CristoMorado_api.dto.*;
import com.CristoMorado_api.CristoMorado_api.entity.*;
import com.CristoMorado_api.CristoMorado_api.exception.DatosInvalidosException;
import com.CristoMorado_api.CristoMorado_api.repository.*;
import com.CristoMorado_api.CristoMorado_api.services.*;
import com.CristoMorado_api.CristoMorado_api.util.Permiso;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * PU-05 – HU-04 a HU-07: Gestión de usuarios (Épica 2, Sprint 2).
 * Registro de alumnos (EstudianteService), padres (PadreFamiliaService), docentes (DocenteService)
 * y personal de la directiva (PersonalService). La cuenta de acceso (CuentaService) se simula.
 */
@DisplayName("PU-05 – HU-04 a HU-07: Gestión de usuarios")
class PU05GestionUsuariosTest {

    private EstudianteRepository estudianteRepository;
    private PadreFamiliaRepository padreRepository;
    private DocenteRepository docenteRepository;
    private PersonalRepository personalRepository;
    private EstudianteService estudiantes;
    private PadreFamiliaService padres;
    private DocenteService docentes;
    private PersonalService personal;
    private final Map<Long, Object> guardados = new HashMap<>();

    @BeforeEach
    void setUp() {
        estudianteRepository = mock(EstudianteRepository.class);
        padreRepository = mock(PadreFamiliaRepository.class);
        docenteRepository = mock(DocenteRepository.class);
        personalRepository = mock(PersonalRepository.class);
        CuentaService cuentaService = mock(CuentaService.class);
        PermisoService permisoService = mock(PermisoService.class);
        when(cuentaService.crear(anyString(), anyString(), any(), anyString(), any(), anyBoolean()))
                .thenAnswer(inv -> Usuario.builder().id(500L).usuario(inv.getArgument(0) + "" + inv.getArgument(1)).build());
        Usuario admin = Usuario.builder().id(1L).usuario("admin123").rol("ADMIN").estado(true).build();
        when(permisoService.exigir(1L, Permiso.USUARIOS)).thenReturn(admin);
        when(permisoService.permisosDe(admin)).thenReturn(Permiso.todos());

        when(estudianteRepository.save(any(Estudiante.class))).thenAnswer(inv -> { Estudiante e = inv.getArgument(0); e.setId(101L); guardados.put(101L, e); return e; });
        when(estudianteRepository.findById(101L)).thenAnswer(inv -> Optional.ofNullable((Estudiante) guardados.get(101L)));
        when(padreRepository.save(any(PadreFamilia.class))).thenAnswer(inv -> { PadreFamilia p = inv.getArgument(0); p.setId(102L); guardados.put(102L, p); return p; });
        when(padreRepository.findById(102L)).thenAnswer(inv -> Optional.ofNullable((PadreFamilia) guardados.get(102L)));
        when(docenteRepository.save(any(Docente.class))).thenAnswer(inv -> { Docente d = inv.getArgument(0); d.setId(103L); return d; });
        when(personalRepository.save(any(Personal.class))).thenAnswer(inv -> { Personal p = inv.getArgument(0); p.setId(104L); return p; });

        LimpiezaUsuarioService limpieza = mock(LimpiezaUsuarioService.class);
        estudiantes = new EstudianteService(estudianteRepository, padreRepository, mock(EstudianteFamiliarRepository.class), cuentaService, limpieza);
        padres = new PadreFamiliaService(padreRepository, estudianteRepository, mock(EstudianteFamiliarRepository.class), cuentaService);
        docentes = new DocenteService(docenteRepository, mock(UsuarioRepository.class), cuentaService);
        personal = new PersonalService(personalRepository, cuentaService, permisoService);
    }

    private static EstudianteRequest alumno(String nombre) {
        return new EstudianteRequest(nombre, "DNI", "70000001", null, LocalDate.of(2015, 3, 10), "Masculino", "Peruana",
                "Av. Los Olivos 123", "987654321", null, null, 2026, "Activo", null);
    }

    private static PadreFamiliaRequest padre(String correo) {
        return new PadreFamiliaRequest("Juan Pérez Rojas", "DNI", "70000002", LocalDate.of(1985, 5, 1), "987654321",
                correo, "Av. Los Olivos 123", "Activo", null);
    }

    private static DocenteRequest docente(String correo) {
        return new DocenteRequest("Carlos Mendoza Ruiz", "DNI", "70000003", null, LocalDate.of(1980, 1, 15), "Masculino",
                "987654321", correo, "Av. Universitaria 456", "Matemática", "Licenciado en Educación",
                LocalDate.of(2020, 3, 1), "Activo", null);
    }

    private static PersonalRequest directiva(String nombres, String correo) {
        return new PersonalRequest("DNI", "70000004", nombres, "Morales", "Guido", correo, "987654321",
                LocalDate.of(1975, 8, 20), "Directora", null);
    }

    @Test
    @DisplayName("PU-05.1 Correo con formato correcto: es aceptado")
    void c1_correoCorrecto() {
        PadreFamiliaResponse p = padres.crear(padre("juan.perez@colegio.com"));

        assertEquals("juan.perez@colegio.com", p.correo());
        verify(padreRepository, times(1)).save(any(PadreFamilia.class));
    }

    @Test
    @DisplayName("PU-05.2 Correo juan#colegio.com: es rechazado (padre, docente y directiva)")
    void c2_correoIncorrecto() {
        assertThrows(DatosInvalidosException.class, () -> padres.crear(padre("juan#colegio.com")),
                "El registro de padres debe rechazar el correo");
        assertThrows(DatosInvalidosException.class, () -> docentes.crear(docente("juan#colegio.com")),
                "El registro de docentes debe rechazar el correo");
        assertThrows(DatosInvalidosException.class, () -> personal.crear(directiva("Paola", "juan#colegio.com"), 1L),
                "El registro de la directiva debe rechazar el correo");
        verify(padreRepository, never()).save(any(PadreFamilia.class));
        verify(docenteRepository, never()).save(any(Docente.class));
    }

    @Test
    @DisplayName("PU-05.3 Datos obligatorios vacíos (nombre): la validación falla")
    void c3_obligatoriosVacios() {
        assertThrows(DatosInvalidosException.class, () -> estudiantes.crear(alumno("   ")),
                "El registro de alumnos debe exigir el nombre");
        assertThrows(DatosInvalidosException.class, () -> personal.crear(directiva("  ", null), 1L),
                "El registro de la directiva debe exigir los nombres");
        verify(estudianteRepository, never()).save(any(Estudiante.class));
    }

    @Test
    @DisplayName("PU-05.4 Datos válidos de cada tipo (alumno, padre, docente y directiva): pasan la validación")
    void c4_datosValidosCadaTipo() {
        assertEquals("Juan Pérez García", estudiantes.crear(alumno("Juan Pérez García")).nombreCompleto());
        assertEquals("Juan Pérez Rojas", padres.crear(padre("juan.perez@gmail.com")).nombreCompleto());
        assertEquals("Carlos Mendoza Ruiz", docentes.crear(docente("carlos.mendoza@colegio.edu.pe")).nombreCompleto());
        personal.crear(directiva("Paola", "direccion@colegio.edu.pe"), 1L);
        verify(personalRepository, times(1)).save(any(Personal.class));
    }
}
