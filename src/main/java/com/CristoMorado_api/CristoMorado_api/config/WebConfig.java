package com.CristoMorado_api.CristoMorado_api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final PermisoInterceptor permisoInterceptor;

    public WebConfig(PermisoInterceptor permisoInterceptor) {
        this.permisoInterceptor = permisoInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(permisoInterceptor).addPathPatterns("/api/**");
    }
}
