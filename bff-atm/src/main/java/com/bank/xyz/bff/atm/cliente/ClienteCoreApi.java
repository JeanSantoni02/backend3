package com.bank.xyz.bff.atm.cliente;

import com.bank.xyz.bff.atm.error.CoreApiNoDisponibleException;
import com.bank.xyz.bff.atm.error.OperacionRechazadaException;
import com.bank.xyz.bff.atm.error.RetiroIndeterminadoException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class ClienteCoreApi {

    private static final String BASE = "/api/v1/cuentas/";

    private final RestClient coreApi;

    public ClienteCoreApi(RestClient coreApi) {
        this.coreApi = coreApi;
    }

    public CoreDto.Saldo saldo(Integer cuentaId) {
        try {
            return coreApi.get()
                    .uri(BASE + cuentaId + "/saldo")
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
        } catch (RestClientResponseException e) {
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new OperacionRechazadaException("CUENTA_NO_ENCONTRADA",
                        "cuenta no encontrada", HttpStatus.NOT_FOUND);
            }
            throw new CoreApiNoDisponibleException("respuesta " + e.getStatusCode());
        } catch (ResourceAccessException e) {
            throw new CoreApiNoDisponibleException("sin conexion con el servicio de dominio");
        }
    }

    public CoreDto.RetiroResponse retirar(Integer cuentaId, CoreDto.RetiroRequest peticion) {
        try {
            return coreApi.post()
                    .uri(BASE + cuentaId + "/retiros")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(peticion)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });

        } catch (RestClientResponseException e) {
            // El servicio respondio con un rechazo explicito: el debito NO se
            // aplico, asi que es seguro traducirlo a un error de negocio.
            throw traducir(e);

        } catch (ResourceAccessException e) {
            // No hubo respuesta. Aqui no se sabe si el debito se aplico, asi que
            // no se puede tratar como un fallo limpio ni entregar el dinero.
            throw new RetiroIndeterminadoException(peticion.referencia());
        }
    }

    private RuntimeException traducir(RestClientResponseException e) {
        CoreDto.ErrorCore error = null;
        try {
            error = e.getResponseBodyAs(CoreDto.ErrorCore.class);
        } catch (Exception ignorada) {
            // El cuerpo no vino en el formato esperado; se usa el estado HTTP.
        }

        if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
            return new OperacionRechazadaException("CUENTA_NO_ENCONTRADA",
                    "cuenta no encontrada", HttpStatus.NOT_FOUND);
        }
        if (e.getStatusCode() == HttpStatus.UNPROCESSABLE_ENTITY) {
            return new OperacionRechazadaException("SALDO_INSUFICIENTE",
                    "saldo insuficiente para el monto solicitado",
                    HttpStatus.UNPROCESSABLE_ENTITY);
        }
        if (e.getStatusCode().is4xxClientError()) {
            String mensaje = (error != null && error.mensaje() != null)
                    ? error.mensaje()
                    : "operacion no permitida";
            String codigo = (error != null && error.error() != null)
                    ? error.error()
                    : "OPERACION_INVALIDA";
            return new OperacionRechazadaException(codigo, mensaje);
        }
        return new CoreApiNoDisponibleException("respuesta " + e.getStatusCode());
    }
}
