package com.bank.xyz.bff.web.cliente;

import com.bank.xyz.bff.web.error.CoreApiNoDisponibleException;
import com.bank.xyz.bff.web.error.RecursoNoEncontradoException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;

@Component
public class ClienteCoreApi {

    private static final String BASE_CUENTAS = "/api/v1/cuentas";

    private final RestClient coreApi;

    public ClienteCoreApi(RestClient coreApi) {
        this.coreApi = coreApi;
    }

    public CoreDto.Cuenta cuenta(Integer cuentaId) {
        return obtener(BASE_CUENTAS + "/" + cuentaId, new ParameterizedTypeReference<>() {
        });
    }

    public CoreDto.Pagina<CoreDto.Cuenta> cuentas(int pagina, int tamano) {
        return obtener(BASE_CUENTAS + "?page=" + pagina + "&size=" + tamano,
                new ParameterizedTypeReference<>() {
                });
    }

    public CoreDto.Pagina<CoreDto.Movimiento> movimientos(Integer cuentaId, Integer anio,
                                                          int pagina, int tamano) {
        String ruta = BASE_CUENTAS + "/" + cuentaId + "/movimientos?page=" + pagina + "&size=" + tamano;
        if (anio != null) {
            ruta = ruta + "&anio=" + anio;
        }
        return obtener(ruta, new ParameterizedTypeReference<>() {
        });
    }

    public List<CoreDto.EstadoAnual> estadosAnuales(Integer cuentaId) {
        return obtener(BASE_CUENTAS + "/" + cuentaId + "/estados-anuales",
                new ParameterizedTypeReference<>() {
                });
    }

    public CoreDto.Pagina<CoreDto.Interes> intereses(Integer cuentaId, int pagina, int tamano) {
        return obtener(BASE_CUENTAS + "/" + cuentaId + "/intereses?page=" + pagina + "&size=" + tamano,
                new ParameterizedTypeReference<>() {
                });
    }

    private <T> T obtener(String ruta, ParameterizedTypeReference<T> tipo) {
        try {
            return coreApi.get().uri(ruta).retrieve().body(tipo);
        } catch (RestClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new RecursoNoEncontradoException("el recurso solicitado no existe");
            }
            throw new CoreApiNoDisponibleException(
                    "el servicio de dominio respondio " + e.getStatusCode());
        } catch (ResourceAccessException e) {
            // Timeout o conexion rechazada. El BFF nunca debe quedar colgado
            // esperando al servicio de dominio.
            throw new CoreApiNoDisponibleException("no se pudo contactar al servicio de dominio");
        }
    }
}
