package co.com.auth.r2dbc;

import co.com.auth.model.role.Role;
import co.com.auth.model.role.gateways.RoleRepository;
import co.com.auth.r2dbc.entity.RolEntity;
import co.com.auth.r2dbc.helper.ReactiveAdapterOperations;

import org.reactivecommons.utils.ObjectMapper;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class RoleReactiveRepositoryAdapter extends ReactiveAdapterOperations<
        Role,
        RolEntity,
        Long,
        RoleReactiveRepository
        >  implements RoleRepository{
    public RoleReactiveRepositoryAdapter(RoleReactiveRepository repository, ObjectMapper mapper) {
        super(repository, mapper, d -> mapper.map(d, Role.class));
    }

}
