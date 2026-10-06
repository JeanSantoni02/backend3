package com.bank.xyz.cuentas.config;

import io.micrometer.observation.ObservationRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class ClientesConfig {

    private static final Logger log = LoggerFactory.getLogger(ClientesConfig.class);
    private static final String REGISTRO = "clientes";

    // @LoadBalanced: la URL usa el nombre en Eureka y se elige una instancia por llamada
    @Bean
    @LoadBalanced
    public RestClient.Builder constructorBalanceado(ObservationRegistry observaciones) {
        return RestClient.builder().observationRegistry(observaciones);
    }

    @Bean
    public RestClient clientes(RestClient.Builder constructorBalanceado,
                               OAuth2AuthorizedClientManager gestor,
                               @Value("${banco.clientes.url:http://servicio-clientes}") String url) {

        SimpleClientHttpRequestFactory fabrica = new SimpleClientHttpRequestFactory();
        fabrica.setConnectTimeout(Duration.ofSeconds(2));
        fabrica.setReadTimeout(Duration.ofSeconds(4));

        var principal = new AnonymousAuthenticationToken(
                "servicio-cuentas", "servicio-cuentas", AuthorityUtils.createAuthorityList("ROLE_SERVICIO"));

        return constructorBalanceado
                .baseUrl(url)
                .requestFactory(fabrica)
                .requestInterceptor((peticion, cuerpo, ejecucion) -> {
                    OAuth2AuthorizedClient token = gestor.authorize(
                            OAuth2AuthorizeRequest.withClientRegistrationId(REGISTRO)
                                    .principal(principal).build());
                    if (token != null) {
                        peticion.getHeaders().setBearerAuth(token.getAccessToken().getTokenValue());
                    } else {
                        log.warn("No se obtuvo token para llamar al servicio de clientes");
                    }
                    return ejecucion.execute(peticion, cuerpo);
                })
                .build();
    }

    // Token de maquina con client_credentials, renovado solo al expirar
    @Bean
    public OAuth2AuthorizedClientManager gestorTokens(ClientRegistrationRepository registros,
                                                      OAuth2AuthorizedClientService servicio) {
        var gestor = new AuthorizedClientServiceOAuth2AuthorizedClientManager(registros, servicio);
        gestor.setAuthorizedClientProvider(
                OAuth2AuthorizedClientProviderBuilder.builder().clientCredentials().build());
        return gestor;
    }
}
