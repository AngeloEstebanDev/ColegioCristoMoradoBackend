package com.CristoMorado_api.CristoMorado_api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuración WebConfig: ajustes generales del backend que Spring carga al arrancar.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final PermisoInterceptor permisoInterceptor;
    private final ActividadInterceptor actividadInterceptor;

    /**
     * Constructor: Spring entrega (inyecta) automáticamente las dependencias que usa esta clase.
     */
    public WebConfig(PermisoInterceptor permisoInterceptor, ActividadInterceptor actividadInterceptor) {
        this.permisoInterceptor = permisoInterceptor;
        this.actividadInterceptor = actividadInterceptor;
    }

    /**
     * Registra los interceptores que revisan cada petición (permisos y actividad reciente).
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Primero se registra la actividad (así también anota los intentos que el permiso rechaza).
        registry.addInterceptor(actividadInterceptor).addPathPatterns("/api/**");
        registry.addInterceptor(permisoInterceptor).addPathPatterns("/api/**");
    }
}
