package com.bank.xyz.auth;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.client.InMemoryRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Configuration
@ConfigurationProperties(prefix = "banco.oauth2")
public class OAuth2Config {

    private String issuer = "http://localhost:9000";
    private List<ClienteOAuth> clientes = new ArrayList<>();

    public static class ClienteOAuth {
        private String id;
        private String secreto;
        private String scopes;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }

        public String getSecreto() { return secreto; }
        public void setSecreto(String secreto) { this.secreto = secreto; }

        public String getScopes() { return scopes; }
        public void setScopes(String scopes) { this.scopes = scopes; }
    }

    public String getIssuer() { return issuer; }
    public void setIssuer(String issuer) { this.issuer = issuer; }

    public List<ClienteOAuth> getClientes() { return clientes; }
    public void setClientes(List<ClienteOAuth> clientes) { this.clientes = clientes; }

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityFilterChain cadenaServidorAutorizacion(HttpSecurity http) throws Exception {
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);
        http.getConfigurer(OAuth2AuthorizationServerConfigurer.class).oidc(Customizer.withDefaults());
        http.exceptionHandling(e -> e.defaultAuthenticationEntryPointFor(
                (req, res, ex) -> res.sendError(401),
                new AntPathRequestMatcher("/**")));
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain cadenaGeneral(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(rutas -> rutas
                        .requestMatchers("/actuator/**", "/.well-known/**",
                                "/oauth2/**").permitAll()
                        .anyRequest().authenticated())
                .csrf(csrf -> csrf.disable());
        return http.build();
    }

    // Los BFF son clientes de maquina, por eso usan client_credentials y no
    // un flujo con intervencion del usuario
    @Bean
    public RegisteredClientRepository clientes(PasswordEncoder codificador) {
        List<RegisteredClient> registrados = clientes.stream().map(c -> {
            RegisteredClient.Builder b = RegisteredClient.withId(UUID.randomUUID().toString())
                    .clientId(c.getId())
                    .clientSecret(codificador.encode(c.getSecreto()))
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                    .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                    .clientSettings(ClientSettings.builder().requireAuthorizationConsent(false).build())
                    .tokenSettings(TokenSettings.builder()
                            .accessTokenTimeToLive(Duration.ofMinutes(30))
                            .build());
            Arrays.stream(c.getScopes().split(",")).map(String::trim).forEach(b::scope);
            return b.build();
        }).toList();

        return new InMemoryRegisteredClientRepository(registrados);
    }

    @Bean
    public PasswordEncoder codificadorClaves() {
        return new BCryptPasswordEncoder();
    }

    // Par de claves RSA para firmar los tokens. En produccion vendria de un
    // almacen de claves, no generado al arrancar
    @Bean
    public JWKSource<SecurityContext> fuenteClaves() {
        KeyPair par = generarClaves();
        RSAPublicKey publica = (RSAPublicKey) par.getPublic();
        RSAPrivateKey privada = (RSAPrivateKey) par.getPrivate();
        RSAKey clave = new RSAKey.Builder(publica)
                .privateKey(privada)
                .keyID(UUID.randomUUID().toString())
                .build();
        return new ImmutableJWKSet<>(new JWKSet(clave));
    }

    private static KeyPair generarClaves() {
        try {
            KeyPairGenerator generador = KeyPairGenerator.getInstance("RSA");
            generador.initialize(2048);
            return generador.generateKeyPair();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar el par de claves RSA", e);
        }
    }

    @Bean
    public JwtDecoder decodificador(JWKSource<SecurityContext> fuente) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(fuente);
    }

    @Bean
    public AuthorizationServerSettings ajustes() {
        return AuthorizationServerSettings.builder().issuer(issuer).build();
    }
}
