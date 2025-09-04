package co.com.auth.api.mapper;

import co.com.auth.api.dto.CreateUserDto;
import co.com.auth.model.role.Role;
import co.com.auth.model.user.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "roleId", source = "rol.idRol")
    CreateUserDto toDto(User entity);

    @Mapping(target = "rol", source = "roleId", qualifiedByName = "mapRoleIdToRole")
    User toEntity(CreateUserDto dto);

    @Named("mapRoleIdToRole")
    default Role mapRoleIdToRole(Long roleId) {
        if (roleId == null) return null;
        return Role.builder().idRol(roleId).build();
    }
}



