package com.bank.xyz.bff.atm.seguridad;

import com.bank.xyz.bff.atm.config.AtmProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

@Component
public class FiltroTerminalAtm extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(FiltroTerminalAtm.class);

    public static final String CABECERA_TERMINAL = "X-ATM-Terminal";
    public static final String CABECERA_CLAVE = "X-ATM-Key";

    private static final List<String> RUTAS_PUBLICAS =
            List.of("/swagger-ui", "/v3/api-docs", "/actuator/health");

    private final AtmProperties propiedades;

    public FiltroTerminalAtm(AtmProperties propiedades) {
        this.propiedades = propiedades;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest peticion) {
        String ruta = peticion.getRequestURI();
        return RUTAS_PUBLICAS.stream().anyMatch(ruta::startsWith);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest peticion, HttpServletResponse respuesta,
                                    FilterChain cadena) throws ServletException, IOException {

        String terminal = peticion.getHeader(CABECERA_TERMINAL);
        String clave = peticion.getHeader(CABECERA_CLAVE);

        if (terminal == null || clave == null || !claveValida(terminal, clave)) {
            // Nunca decir si fallo el terminal o la clave: eso permitiria
            // enumerar terminales validos probando de a uno.
            log.warn("Intento de acceso rechazado desde {} para terminal {}",
                    peticion.getRemoteAddr(), terminal);
            respuesta.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            respuesta.setContentType("application/json;charset=UTF-8");
            respuesta.getWriter().write(
                    "{\"codigo\":\"NO_AUTORIZADO\",\"mensaje\":\"terminal no autorizado\"}");
            return;
        }

        var autenticacion = new UsernamePasswordAuthenticationToken(
                terminal, null, AuthorityUtils.createAuthorityList("ROLE_CAJERO"));
        SecurityContextHolder.getContext().setAuthentication(autenticacion);

        try {
            cadena.doFilter(peticion, respuesta);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private boolean claveValida(String terminal, String clavePresentada) {
        String claveEsperada = propiedades.getTerminales().get(terminal);
        if (claveEsperada == null) {
            return false;
        }
        // Comparacion en tiempo constante: con equals(), el tiempo de respuesta
        // varia segun cuantos caracteres coinciden y filtra informacion.
        return MessageDigest.isEqual(
                claveEsperada.getBytes(StandardCharsets.UTF_8),
                clavePresentada.getBytes(StandardCharsets.UTF_8));
    }
}
