package org.phong.zenflow.user.infrastructure.mapstruct;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.phong.zenflow.user.dtos.CreateUserRequest;
import org.phong.zenflow.user.dtos.UpdateUserRequest;
import org.phong.zenflow.user.dtos.UserDto;
import org.phong.zenflow.user.infrastructure.persistence.entities.User;
import org.phong.zenflow.user.subdomain.role.infrastructure.mapstruct.RoleMapper;

import java.util.List;

@Mapper(componentModel = "spring", uses = {RoleMapper.class}, unmappedTargetPolicy = org.mapstruct.ReportingPolicy.IGNORE)
public interface UserMapper {
    @Mapping(target = "role", source = "role")
    UserDto toDto(User user);

    List<UserDto> toDtoList(List<User> users);

    User toEntity(CreateUserRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(UpdateUserRequest request, @MappingTarget User user);
}
