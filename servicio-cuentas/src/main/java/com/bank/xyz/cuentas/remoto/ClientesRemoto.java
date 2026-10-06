package com.bank.xyz.cuentas.remoto;

import com.bank.xyz.cuentas.error.ClienteInexistenteException;
import com.bank.xyz.cuentas.error.ManejadorErrores.ServicioNoDisponibleException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class ClientesRemoto {

    private static final Logger log = LoggerFactory.getLogger(ClientesRemoto.class);
    private static final String INSTANCIA = "clientes";

    public record ClienteResumen(Long id, String rut, String nombre, String estado) {
        public boolean activo() { return "ACTIVO".equals(estado); }
    }

    private final RestClient clientes;

    public ClientesRemoto(RestClient clientes) {
        this.clientes = clientes;
    }

    @CircuitBreaker(name = INSTANCIA, fallbackMethod = "sinRespuesta")
    @Retry(name = INSTANCIA)
    public ClienteResumen obtener(Long clienteId) {
        try {
            return clientes.get()
                    .uri("/api/v1/clientes/{id}", clienteId)
                    .retrieve()
                    .body(ClienteResumen.class);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new ClienteInexistenteException(clienteId);
            }
            throw e;
        }
    }

    // Sin confirmar al titular no se abre la cuenta: abrirla igual podria dejarla huerfana
    private ClienteResumen sinRespuesta(Long clienteId, Throwable causa) {
        if (causa instanceof ClienteInexistenteException inexistente) {
            throw inexistente;
        }
        log.error("Servicio de clientes sin respuesta al validar {}: {}", clienteId, causa.toString());
        throw new ServicioNoDisponibleException(
                "no se pudo validar al cliente en este momento; intente nuevamente en unos minutos");
    }
}
