package co.com.auth.security;

import co.com.auth.model.security.AuthToken;
import co.com.auth.model.security.gateways.TokenProvider;
import co.com.auth.model.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtProviderAdapter implements TokenProvider {
    private final JwtEncoder encoder;

    @Value("${security.jwt.ttl-seconds}")
    long ttl;

    @Override
    public Mono<AuthToken> createAccessToken(User u) {
        var now = Instant.now();
        var exp = now.plusSeconds(ttl);

        var claims = JwtClaimsSet.builder()
                .issuer("auth-service")
                .issuedAt(now)
                .expiresAt(exp)
                .subject(u.getUserId())                      //
                .claim("email", u.getEmail())
                .claim("numberDocument", u.getNumberDocument())
                .claim("roles", List.of(u.getRol().getName()))
                .build();

        Jwt jwt = encoder.encode(JwtEncoderParameters.from(claims));
        String tokenValue = jwt.getTokenValue();
        return Mono.just(new AuthToken(tokenValue, exp));
    }
}
