package com.bank.xyz.bff.mobile.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SeguridadConfig {

    // Este BFF es la capa publica: no autentica al visitante. La credencial
    // OAuth2 que usa es la suya propia, hacia el servicio de dominio, y la
    // aplica el interceptor de salida. Sin esta cadena, Spring Security
    // protegeria todo con su formulario de login por defecto.
    @Bean
    public SecurityFilterChain cadena(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(rutas -> rutas.anyRequest().permitAll());
        return http.build();
    }
}
