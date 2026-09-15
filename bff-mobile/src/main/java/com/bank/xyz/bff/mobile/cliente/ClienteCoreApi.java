package com.bank.xyz.bff.mobile.cliente;

import com.bank.xyz.bff.mobile.error.CoreApiNoDisponibleException;
import com.bank.xyz.bff.mobile.error.RecursoNoEncontradoException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;

@Component
public class ClienteCoreApi {

    private static final String BASE = "/api/v1/cuentas/";

    private final RestClient coreApi;

    public ClienteCoreApi(RestClient coreApi) {
        this.coreApi = coreApi;
    }

    public CoreDto.Cuenta cuenta(Integer cuentaId) {
        return obtener(BASE + cuentaId, new ParameterizedTypeReference<>() {
        });
    }

    public CoreDto.Saldo saldo(Integer cuentaId) {
        return obtener(BASE + cuentaId + "/saldo", new ParameterizedTypeReference<>() {
        });
    }

    public List<CoreDto.Movimiento> ultimosMovimientos(Integer cuentaId, int cantidad) {
        return obtener(BASE + cuentaId + "/movimientos/ultimos?cantidad=" + cantidad,
                new ParameterizedTypeReference<>() {
                });
    }

    private <T> T obtener(String ruta, ParameterizedTypeReference<T> tipo) {
        try {
            return coreApi.get().uri(ruta).retrieve().body(tipo);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new RecursoNoEncontradoException("cuenta no encontrada");
            }
            throw new CoreApiNoDisponibleException("respuesta " + e.getStatusCode());
        } catch (ResourceAccessException e) {
            throw new CoreApiNoDisponibleException("sin conexion con el servicio de dominio");
        }
    }
}
