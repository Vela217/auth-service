package co.com.auth.r2dbc;

import co.com.auth.model.role.Role;
import co.com.auth.model.user.User;
import co.com.auth.model.user.gateways.UserRepository;
import co.com.auth.r2dbc.entity.UserEntity;
import co.com.auth.r2dbc.helper.ReactiveAdapterOperations;

import reactor.core.publisher.Mono;

import org.reactivecommons.utils.ObjectMapper;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class UserReactiveRepositoryAdapter extends ReactiveAdapterOperations<
        User,
        UserEntity,
        UUID,
        UserReactiveRepository
> implements UserRepository {
    public UserReactiveRepositoryAdapter(UserReactiveRepository repository, ObjectMapper mapper) {
        super(repository, mapper, d -> mapper.map(d, User.class));
    }


    @Override
    public Mono<Boolean> existsByEmail(String email) {
        return repository.existsByEmail(email);
    }

    @Override
    public Mono<User> findByNumberDocument(String number) {
        return repository.findByNumberDocument(number);
    }

    @Override
    public Mono<User> findByEmail(String email) {
        return repository.findByEmail(email) // Mono<UserEntity>
                .map(entity -> {
                    User u = mapper.map(entity, User.class);
                    Long roleId = entity.getRoleId();
                    if (roleId != null) {
                        u.setRol(Role.builder().idRol(roleId).build()); // solo idRol
                    }
                    return u;
                });
}

    @Override
    public Mono<User> save(User user) {
        UserEntity entity = mapper.map(user, UserEntity.class);
        Long roleId = user != null && user.getRol() != null ? user.getRol().getIdRol() : null;
        entity.setRoleId(roleId);

        return repository.save(entity)
                .map(saved -> {
                    User mapped = mapper.map(saved, User.class);
                    if (saved.getRoleId() != null) {
                        mapped.setRol(Role.builder().idRol(saved.getRoleId()).build());
                    }
                    return mapped;
                });
    }
}
