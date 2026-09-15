package com.bank.xyz.bff.atm.seguridad;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SeguridadConfig {

    private final FiltroTerminalAtm filtroTerminal;

    public SeguridadConfig(FiltroTerminalAtm filtroTerminal) {
        this.filtroTerminal = filtroTerminal;
    }

    @Bean
    public SecurityFilterChain cadenaDeFiltros(HttpSecurity http) throws Exception {
        http
                // Sin CSRF ni sesiones: el cajero es un cliente de maquina que se
                // identifica en cada peticion con su credencial, no un navegador
                // con cookie de sesion.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(rutas -> rutas
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html",
                                "/v3/api-docs/**", "/actuator/health").permitAll()
                        .requestMatchers("/bff/atm/**").hasRole("CAJERO")
                        .anyRequest().denyAll())
                .addFilterBefore(filtroTerminal, UsernamePasswordAuthenticationFilter.class)
                .headers(cabeceras -> cabeceras
                        .frameOptions(marco -> marco.deny())
                        .cacheControl(cache -> {
                        }));

        return http.build();
    }
}
