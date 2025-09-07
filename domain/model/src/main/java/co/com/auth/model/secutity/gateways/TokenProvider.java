package co.com.auth.model.secutity.gateways;

import co.com.auth.model.secutity.AuthToken;
import co.com.auth.model.user.User;
import reactor.core.publisher.Mono;

public interface TokenProvider {

    Mono<AuthToken> createAccessToken(User user);
}
