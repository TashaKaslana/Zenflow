package org.phong.zenflow.user.infrastructure.mapstruct;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.phong.zenflow.user.dtos.CreateUserRequest;
import org.phong.zenflow.user.dtos.UserDto;
import org.phong.zenflow.user.infrastructure.persistence.entities.User;
import org.phong.zenflow.user.subdomain.role.infrastructure.mapstruct.RoleMapper;

import java.util.List;

@Mapper(componentModel = "spring", uses = {RoleMapper.class})
public interface UserMapper {

    @Mapping(target = "role", source = "role")
    UserDto toDto(User user);

    List<UserDto> toDtoList(List<User> users);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "role", ignore = true)
    User toEntity(CreateUserRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "role", ignore = true)
    void updateEntity(CreateUserRequest request, @MappingTarget User user);
}
