package co.com.auth.usecase.login;

import co.com.auth.model.security.AuthToken;
import co.com.auth.model.security.gateways.PasswordEncoderGateway;
import co.com.auth.model.role.gateways.RoleRepository;
import co.com.auth.model.security.gateways.TokenProvider;
import co.com.auth.model.user.User;
import co.com.auth.model.user.gateways.UserRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class LoginUseCase {
    private final UserRepository users;
    private final RoleRepository roleRepository;
    private final PasswordEncoderGateway passwords;
    private final TokenProvider tokens;

    public Mono<AuthToken> login(String email, String password) {
        return users.findByEmail(email)
                .switchIfEmpty(Mono.error(badCredentials()))
                .filterWhen(u -> passwords.matches(password, u.getPassword()))
                .switchIfEmpty(Mono.error(badCredentials()))
                .flatMap(this::attachFullRole)
                .flatMap(tokens::createAccessToken);
    }

    private Mono<User> attachFullRole(User user) {
        Long roleId = (user.getRol() != null) ? user.getRol().getIdRol() : null;
        if (roleId == null) return Mono.error(new IllegalStateException("Usuario sin rol asignado"));

        return roleRepository.findById(roleId)
                .switchIfEmpty(Mono.error(new IllegalStateException("Rol no encontrado")))
                .map(role -> { user.setRol(role); return user; });
    }

    private static IllegalArgumentException badCredentials() {
        return new IllegalArgumentException("Usuario o contraseña incorrecta");
    }
}
