package co.com.auth.model.security.gateways;

import co.com.auth.model.security.AuthToken;
import co.com.auth.model.user.User;
import reactor.core.publisher.Mono;

public interface TokenProvider {

    Mono<AuthToken> createAccessToken(User user);
}
