package com.bank.xyz.pagos.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

// Identifica que instancia atendio la peticion: hace visible el balanceo de carga
@Component
public class InstanciaFiltro extends OncePerRequestFilter {

    private final Environment entorno;
    private String instancia;

    public InstanciaFiltro(Environment entorno) {
        this.entorno = entorno;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest peticion, HttpServletResponse respuesta,
                                    FilterChain cadena) throws ServletException, IOException {
        respuesta.setHeader("X-Atendido-Por", instancia());
        cadena.doFilter(peticion, respuesta);
    }

    private String instancia() {
        if (instancia == null) {
            String host;
            try {
                host = InetAddress.getLocalHost().getHostName();
            } catch (UnknownHostException e) {
                host = "desconocido";
            }
            instancia = host + ":" + entorno.getProperty("local.server.port", "?");
        }
        return instancia;
    }
}
