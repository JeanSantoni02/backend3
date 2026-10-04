package com.bank.xyz.bff.mobile.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

@Configuration
public class OAuth2ClientConfig {

    private static final Logger log = LoggerFactory.getLogger(OAuth2ClientConfig.class);
    public static final String REGISTRO = "core-api";

    // Cliente de maquina: obtiene el token con client_credentials
    @Bean
    public OAuth2AuthorizedClientManager gestorTokens(
            ClientRegistrationRepository registros,
            OAuth2AuthorizedClientService servicio) {

        var proveedor = OAuth2AuthorizedClientProviderBuilder.builder()
                .clientCredentials()
                .build();

        var gestor = new AuthorizedClientServiceOAuth2AuthorizedClientManager(registros, servicio);
        gestor.setAuthorizedClientProvider(proveedor);
        return gestor;
    }

    // Agrega el bearer token a cada llamada al servicio de dominio
    @Bean
    public ClientHttpRequestInterceptor interceptorOAuth2(OAuth2AuthorizedClientManager gestor) {
        var principal = new AnonymousAuthenticationToken(
                "bff-mobile", "bff-mobile", AuthorityUtils.createAuthorityList("ROLE_SERVICIO"));

        return (peticion, cuerpo, ejecucion) -> {
            OAuth2AuthorizedClient cliente = gestor.authorize(
                    OAuth2AuthorizeRequest.withClientRegistrationId(REGISTRO)
                            .principal(principal)
                            .build());

            if (cliente != null && cliente.getAccessToken() != null) {
                peticion.getHeaders().setBearerAuth(cliente.getAccessToken().getTokenValue());
            } else {
                log.warn("No se obtuvo token OAuth2 para el registro '{}'", REGISTRO);
            }
            return ejecucion.execute(peticion, cuerpo);
        };
    }
}
