package co.com.auth.model.secutity.gateways;

import reactor.core.publisher.Mono;

public interface PasswordEncoderGateway  {

    Mono<String> encode(String raw);
    Mono<Boolean> matches(String raw, String encoded);
}
