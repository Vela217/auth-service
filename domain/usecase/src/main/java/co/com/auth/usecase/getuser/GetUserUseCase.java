package co.com.auth.usecase.getuser;

import co.com.auth.model.role.gateways.RoleRepository;
import co.com.auth.model.user.User;
import co.com.auth.model.user.gateways.UserRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class GetUserUseCase {

    private final UserRepository userRepository;

    public Mono<User> findByNumberDocument(String number){
        return userRepository.findByNumberDocument(number)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Usuario no encontrado")));
                };

    }

