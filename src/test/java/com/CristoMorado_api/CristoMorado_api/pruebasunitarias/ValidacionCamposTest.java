package com.CristoMorado_api.CristoMorado_api.pruebasunitarias;

import com.CristoMorado_api.CristoMorado_api.exception.DatosInvalidosException;
import com.CristoMorado_api.CristoMorado_api.util.Campos;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas complementarias de las reglas de validación de campos (util/Campos),
 * que usan los formularios de alumnos, padres, docentes, directiva y perfil.
 */
@DisplayName("VC – Reglas de validación de campos")
class ValidacionCamposTest {

    private static String error(Runnable r) {
        return assertThrows(DatosInvalidosException.class, r::run).getMessage();
    }

    @Test
    @DisplayName("VC-01 Teléfono: solo 9 dígitos que empiezan con 9")
    void c1_telefono() {
        assertEquals("987654321", Campos.telefono("987 654 321", false));
        assertEquals("987654321", Campos.telefono("987-654-321", true));
        assertNull(Campos.telefono("  ", false));
        assertEquals("El teléfono es obligatorio.", error(() -> Campos.telefono("", true)));
        assertEquals("El teléfono debe tener 9 dígitos.", error(() -> Campos.telefono("98765432", false)));
        assertEquals("El teléfono debe tener 9 dígitos.", error(() -> Campos.telefono("9876543210", false)));
        assertEquals("El teléfono solo puede tener números.", error(() -> Campos.telefono("98765432a", false)));
        assertEquals("El teléfono debe empezar con 9 (celular).", error(() -> Campos.telefono("187654321", false)));
    }

    @Test
    @DisplayName("VC-02 Nombre completo: solo letras, nombre y apellido, 5 a 100 caracteres")
    void c2_nombreCompleto() {
        assertEquals("María José Ñahui", Campos.nombreCompleto("  María   José Ñahui ", "vacío"));
        assertEquals("Juan O'Connor-Díaz", Campos.nombreCompleto("Juan O'Connor-Díaz", "vacío"));
        assertEquals("vacío", error(() -> Campos.nombreCompleto("   ", "vacío")));
        assertEquals("El nombre solo puede tener letras y espacios.", error(() -> Campos.nombreCompleto("Juan P3rez", "vacío")));
        assertEquals("Escribe el nombre y al menos un apellido.", error(() -> Campos.nombreCompleto("Juan", "vacío")));
        assertTrue(error(() -> Campos.nombreCompleto("Ana " + "B".repeat(100), "vacío")).startsWith("El nombre debe tener entre 5 y 100"));
    }

    @Test
    @DisplayName("VC-03 Correo: formato usuario@dominio.ext y hasta 150 caracteres")
    void c3_correo() {
        assertEquals("juan.perez@colegio.edu.pe", Campos.correo(" Juan.Perez@Colegio.edu.pe ", "El correo"));
        assertNull(Campos.correo("", "El correo"));
        assertEquals("El correo no es válido.", error(() -> Campos.correo("juan#colegio.com", "El correo")));
        assertEquals("El correo no es válido.", error(() -> Campos.correo("juan@colegio", "El correo")));
        assertTrue(error(() -> Campos.correo("a".repeat(150) + "@x.com", "El correo")).contains("150"));
    }

    @Test
    @DisplayName("VC-04 Fecha de nacimiento: no futura y con edad válida")
    void c4_fechaNacimiento() {
        LocalDate hoy = LocalDate.now();
        assertEquals(hoy.minusYears(10), Campos.fechaNacimiento(hoy.minusYears(10), 3, 20));
        assertEquals("La fecha de nacimiento no puede ser futura.", error(() -> Campos.fechaNacimiento(hoy.plusDays(1), 3, 20)));
        assertTrue(error(() -> Campos.fechaNacimiento(hoy.minusYears(1), 3, 20)).contains("entre 3 y 20"));
        assertTrue(error(() -> Campos.fechaNacimiento(hoy.minusYears(15), 18, 100)).contains("entre 18 y 100"));
    }

    @Test
    @DisplayName("VC-05 Textos, opciones, números y contraseña")
    void c5_otros() {
        assertEquals("Av. Los Olivos 123", Campos.direccion("  Av.  Los Olivos 123 "));
        assertEquals("La dirección debe tener al menos 5 caracteres.", error(() -> Campos.direccion("Av")));
        assertEquals("Femenino", Campos.opcion("femenino", List.of("Masculino", "Femenino"), "el género"));
        assertTrue(error(() -> Campos.opcion("Otro", List.of("Masculino", "Femenino"), "el género")).startsWith("El género no es válido"));
        assertEquals(2024, Campos.entero(2024, 2000, 2027, "el año de ingreso").intValue());
        assertEquals("El año de ingreso debe estar entre 2000 y 2027.", error(() -> Campos.entero(1999, 2000, 2027, "el año de ingreso")));
        assertEquals("En los nombres solo se permiten letras y espacios.", error(() -> Campos.parteNombre("Ana2", "los nombres", 100, true)));
        assertEquals("La contraseña no puede tener espacios.", error(() -> Campos.password("mi clave")));
        assertEquals("La contraseña debe tener al menos 4 caracteres.", error(() -> Campos.password("123")));
    }
}
