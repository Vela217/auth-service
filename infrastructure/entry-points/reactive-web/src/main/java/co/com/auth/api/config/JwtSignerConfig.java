package co.com.auth.api.config;

import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

@Slf4j
@Configuration
public class JwtSignerConfig {

    @Bean
    JwtEncoder jwtEncoder(@Value("${security.jwt.public}") RSAPublicKey pub,
                          @Value("${security.jwt.private}") RSAPrivateKey pk) {
        log.info("Estoy en jwtEncoder de JwtSignerConfig");
        var jwk = new RSAKey.Builder(pub).privateKey(pk).build();
        var jwks = new ImmutableJWKSet<>(
                new com.nimbusds.jose.jwk.JWKSet(jwk));
        return new NimbusJwtEncoder(jwks);
    }
}