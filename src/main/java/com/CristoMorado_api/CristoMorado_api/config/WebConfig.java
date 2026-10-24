package com.CristoMorado_api.CristoMorado_api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final PermisoInterceptor permisoInterceptor;
    private final ActividadInterceptor actividadInterceptor;

    public WebConfig(PermisoInterceptor permisoInterceptor, ActividadInterceptor actividadInterceptor) {
        this.permisoInterceptor = permisoInterceptor;
        this.actividadInterceptor = actividadInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Primero se registra la actividad (así también anota los intentos que el permiso rechaza).
        registry.addInterceptor(actividadInterceptor).addPathPatterns("/api/**");
        registry.addInterceptor(permisoInterceptor).addPathPatterns("/api/**");
    }
}
