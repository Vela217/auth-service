package co.com.auth.usecase.user;

import co.com.auth.model.role.gateways.RoleRepository;
import co.com.auth.model.user.User;
import co.com.auth.model.user.gateways.UserRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class CreateUseCase {


    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public Mono<User> registerUser(User u) {
        return roleRepository.findById(u.getRol().getIdRol())
                .switchIfEmpty(Mono.error(new IllegalArgumentException("El rol no existe")))
                .flatMap(role -> {
                    User toSave = u.toBuilder().rol(role).build();
                    return userRepository.existsByEmail(toSave.getEmail())
                            .flatMap(exists -> exists
                                    ? Mono.error(new IllegalStateException("El correo ya esta en uso"))
                                    : userRepository.save(toSave));
                });
    }
}
