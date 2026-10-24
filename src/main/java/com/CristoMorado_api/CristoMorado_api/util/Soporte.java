package com.CristoMorado_api.CristoMorado_api.util;

/** Contacto de soporte del sistema (WhatsApp). El frontend usa el mismo número (environment.soporteWhatsapp). */
public final class Soporte {

    public static final String WHATSAPP = "+51 983 046 253";
    /** Frase para los mensajes de error: "... escribe a soporte por WhatsApp al +51 983 046 253." */
    public static final String ESCRIBE_A_SOPORTE = "escribe a soporte por WhatsApp al " + WHATSAPP;

    private Soporte() {}
}
