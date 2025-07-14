package org.phong.zenflow.setting.infrastructure.mapstruct;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import org.phong.zenflow.setting.dto.CreateUserSettingRequest;
import org.phong.zenflow.setting.dto.UpdateUserSettingRequest;
import org.phong.zenflow.setting.dto.UserSettingDto;
import org.phong.zenflow.setting.infrastructure.persistence.entity.UserSetting;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserSettingMapper {
    @Mapping(source = "user.id", target = "userId")
    UserSettingDto toDto(UserSetting userSetting);

    @Mapping(source = "userId", target = "user.id")
    UserSetting toEntity(CreateUserSettingRequest createUserSettingRequest);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    UserSetting partialUpdate(UpdateUserSettingRequest updateUserSettingRequest, @MappingTarget UserSetting userSetting);
}
