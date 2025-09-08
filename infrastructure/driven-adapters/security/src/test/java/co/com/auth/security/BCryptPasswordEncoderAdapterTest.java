package co.com.auth.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;

class BCryptPasswordEncoderAdapterTest {

    private final BCryptPasswordEncoderAdapter adapter = new BCryptPasswordEncoderAdapter();

    @Test
    @DisplayName("encode y matches -> genera hash y valida correctamente (true/false)")
    void encodeAndMatches() {
        // Arrange
        String raw = "Secreta#123";
        Mono<String> encodedMono = adapter.encode(raw);

        // Act & Assert: encode
        final String[] encodedHolder = new String[1];

        StepVerifier.create(encodedMono)
                .assertNext(encoded -> {
                    assertNotNull(encoded);
                    assertNotEquals(raw, encoded, "El hash no debe ser igual al raw");
                    encodedHolder[0] = encoded;
                })
                .verifyComplete();

        String encoded = encodedHolder[0];

        // Act & Assert: matches correcto
        StepVerifier.create(adapter.matches(raw, encoded))
                .expectNext(true)
                .verifyComplete();

        // Act & Assert: matches incorrecto
        StepVerifier.create(adapter.matches("otraClave", encoded))
                .expectNext(false)
                .verifyComplete();
    }
}
