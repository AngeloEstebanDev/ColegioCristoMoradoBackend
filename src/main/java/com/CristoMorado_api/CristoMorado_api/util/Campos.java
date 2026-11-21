package com.CristoMorado_api.CristoMorado_api.util;

import com.CristoMorado_api.CristoMorado_api.exception.DatosInvalidosException;

import java.time.LocalDate;
import java.time.Period;
import java.util.Collection;

public final class Campos {

    private static final String LETRAS = "[\\p{L}][\\p{L} .'’-]*";

    public static final int NOMBRE_MAX = 100;
    public static final int DIRECCION_MAX = 200;
    public static final int CORREO_MAX = 150;

    private Campos() {}

    public static String limpio(String s) {
        if (s == null) return null;
        String t = s.trim().replaceAll("\\s+", " ");
        return t.isEmpty() ? null : t;
    }

    public static String nombreCompleto(String nombre, String vacio) {
        String n = limpio(nombre);
        if (n == null) throw new DatosInvalidosException(vacio);
        if (!n.matches(LETRAS)) throw new DatosInvalidosException("El nombre solo puede tener letras y espacios.");
        if (n.split(" ").length < 2) throw new DatosInvalidosException("Escribe el nombre y al menos un apellido.");
        if (n.length() < 5 || n.length() > NOMBRE_MAX) {
            throw new DatosInvalidosException("El nombre debe tener entre 5 y " + NOMBRE_MAX + " caracteres.");
        }
        return n;
    }

    public static String parteNombre(String valor, String etiqueta, int max, boolean obligatorio) {
        String v = limpio(valor);
        if (v == null) {
            if (obligatorio) throw new DatosInvalidosException("Escribe " + etiqueta + ".");
            return null;
        }
        if (!v.matches(LETRAS)) throw new DatosInvalidosException("En " + etiqueta + " solo se permiten letras y espacios.");
        if (v.length() < 2 || v.length() > max) {
            throw new DatosInvalidosException("En " + etiqueta + " escribe entre 2 y " + max + " caracteres.");
        }
        return v;
    }

    public static String telefono(String telefono, boolean obligatorio) {
        String t = telefono == null ? "" : telefono.replaceAll("[\\s-]", "");
        if (t.isEmpty()) {
            if (obligatorio) throw new DatosInvalidosException("El teléfono es obligatorio.");
            return null;
        }
        if (!t.matches("\\d+")) throw new DatosInvalidosException("El teléfono solo puede tener números.");
        if (t.length() != 9) throw new DatosInvalidosException("El teléfono debe tener 9 dígitos.");
        if (t.charAt(0) != '9') throw new DatosInvalidosException("El teléfono debe empezar con 9 (celular).");
        return t;
    }

    public static String correo(String correo, String etiqueta) {
        String c = correo == null ? null : correo.trim();
        if (c == null || c.isEmpty()) return null;
        if (c.length() > CORREO_MAX) throw new DatosInvalidosException(etiqueta + " no puede superar los " + CORREO_MAX + " caracteres.");
        if (!c.matches("[^\\s@]+@[^\\s@]+\\.[A-Za-z]{2,}")) throw new DatosInvalidosException(etiqueta + " no es válido.");
        return c.toLowerCase();
    }

    public static String texto(String valor, String etiqueta, int min, int max, boolean obligatorio) {
        String v = valor == null ? null : valor.trim();
        if (v == null || v.isEmpty()) {
            if (obligatorio) throw new DatosInvalidosException("Escribe " + etiqueta + ".");
            return null;
        }
        if (v.length() < min) throw new DatosInvalidosException(mayuscula(etiqueta) + " debe tener al menos " + min + " caracteres.");
        if (v.length() > max) throw new DatosInvalidosException(mayuscula(etiqueta) + " admite hasta " + max + " caracteres.");
        return v;
    }

    public static String direccion(String direccion) {
        return texto(limpio(direccion), "la dirección", 5, DIRECCION_MAX, false);
    }

    public static String opcion(String valor, Collection<String> permitidos, String etiqueta) {
        String v = limpio(valor);
        if (v == null) return null;
        for (String p : permitidos) if (p.equalsIgnoreCase(v)) return p;
        throw new DatosInvalidosException(mayuscula(etiqueta) + " no es válido. Usa: " + String.join(", ", permitidos) + ".");
    }

    public static LocalDate fechaNacimiento(LocalDate fecha, int minEdad, int maxEdad) {
        if (fecha == null) return null;
        LocalDate hoy = LocalDate.now();
        if (fecha.isAfter(hoy)) throw new DatosInvalidosException("La fecha de nacimiento no puede ser futura.");
        int edad = Period.between(fecha, hoy).getYears();
        if (edad < minEdad || edad > maxEdad) {
            throw new DatosInvalidosException("Revisa la fecha de nacimiento: la edad debe estar entre " + minEdad + " y " + maxEdad + " años.");
        }
        return fecha;
    }

    public static Integer entero(Integer valor, int min, int max, String etiqueta) {
        if (valor == null) return null;
        if (valor < min || valor > max) {
            throw new DatosInvalidosException(mayuscula(etiqueta) + " debe estar entre " + min + " y " + max + ".");
        }
        return valor;
    }

    public static LocalDate noFutura(LocalDate fecha, String etiqueta) {
        if (fecha != null && fecha.isAfter(LocalDate.now())) {
            throw new DatosInvalidosException(mayuscula(etiqueta) + " no puede ser futura.");
        }
        return fecha;
    }

    public static String password(String password) {
        if (password == null || password.isEmpty()) return null;
        if (password.length() < 4) throw new DatosInvalidosException("La contraseña debe tener al menos 4 caracteres.");
        if (password.length() > 64) throw new DatosInvalidosException("La contraseña admite hasta 64 caracteres.");
        if (password.matches(".*\\s.*")) throw new DatosInvalidosException("La contraseña no puede tener espacios.");
        return password;
    }

    private static String mayuscula(String s) {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
