package com.CristoMorado_api.CristoMorado_api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuración SecurityConfig: ajustes generales del backend que Spring carga al arrancar.
 */
@Configuration
public class SecurityConfig {

    /**
     * Crea y registra en Spring el objeto «security filter chain» para que se use en todo el backend.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/**").permitAll()
                .anyRequest().authenticated()
            );

        return http.build();
    }
}