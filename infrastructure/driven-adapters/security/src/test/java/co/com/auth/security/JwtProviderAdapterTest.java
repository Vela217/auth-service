package co.com.auth.security;

import co.com.auth.model.role.Role;
import co.com.auth.model.security.AuthToken;
import co.com.auth.model.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtProviderAdapterTest {

    @Mock
    private JwtEncoder jwtEncoder;

    @InjectMocks
    private JwtProviderAdapter adapter;

    @Captor
    private ArgumentCaptor<JwtEncoderParameters> paramsCaptor;

    @BeforeEach
    void init() {
        ReflectionTestUtils.setField(adapter, "ttl", 3600L);
    }

    private User sampleUser() {
        return User.builder()
                .userId("U-123")
                .email("user@test.com")
                .numberDocument("CC-987654")
                .rol(Role.builder().name("ADMIN").idRol(1L).build())
                .build();
    }

    @Test
    @DisplayName("createAccessToken -> construye claims y retorna token con exp ≈ now + ttl")
    void createAccessToken_buildsClaims_andReturnsToken() {
        // Arrange
        User u = sampleUser();

        // Simulamos que el encoder retorna un Jwt con el mismo iat/exp de las claims que se envían
        when(jwtEncoder.encode(any(JwtEncoderParameters.class)))
                .thenAnswer(inv -> {
                    JwtEncoderParameters p = inv.getArgument(0);
                    JwtClaimsSet claims = p.getClaims();
                    return new Jwt(
                            "header.payload.signature",
                            claims.getIssuedAt(),
                            claims.getExpiresAt(),
                            Map.of("alg", "RS256"),
                            claims.getClaims()
                    );
                });

        Instant before = Instant.now();

        // Act
        StepVerifier.create(adapter.createAccessToken(u))
                // Assert
                .assertNext(token -> {
                    assertNotNull(token);
                    assertEquals("header.payload.signature", token.getToken());

                    // exp debería ser ~ now + ttl (3600s)
                    Instant expectedLowerBound = before.plusSeconds(3600 - 2); // tolerancia -2s
                    Instant expectedUpperBound = Instant.now().plusSeconds(3600 + 2); // tolerancia +2s
                    assertTrue(!token.getExpiresAt().isBefore(expectedLowerBound)
                                    && !token.getExpiresAt().isAfter(expectedUpperBound),
                            "expiresAt debe estar dentro de la ventana esperada alrededor de now+ttl");
                })
                .verifyComplete();

        // Verificamos que el encode se llamó y validamos las claims
        verify(jwtEncoder).encode(paramsCaptor.capture());
        JwtClaimsSet claims = paramsCaptor.getValue().getClaims();

        assertEquals("auth-service",  claims.getClaim("iss"));
        assertEquals("U-123", claims.getSubject());
        assertEquals("user@test.com", claims.getClaim("email"));
        assertEquals("CC-987654", claims.getClaim("numberDocument"));

        @SuppressWarnings("unchecked")
        List<String> roles = (List<String>) claims.getClaim("roles");
        assertEquals(List.of("ADMIN"), roles);

        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiresAt());
        assertTrue(Duration.between(claims.getIssuedAt(), claims.getExpiresAt()).getSeconds() >= 3598,
                "La diferencia exp-iat debe ser aproximadamente el TTL configurado");
        verifyNoMoreInteractions(jwtEncoder);
    }
}
