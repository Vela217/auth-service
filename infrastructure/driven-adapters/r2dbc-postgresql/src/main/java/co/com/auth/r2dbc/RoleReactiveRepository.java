package co.com.auth.r2dbc;

import co.com.auth.r2dbc.entity.RolEntity;
import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface RoleReactiveRepository extends ReactiveCrudRepository<RolEntity, Long>, ReactiveQueryByExampleExecutor<RolEntity> {

}
