package org.phong.zenflow.notification.infrastructure.mapstruct;

import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import org.phong.zenflow.notification.dto.CreateNotificationRequest;
import org.phong.zenflow.notification.dto.NotificationDto;
import org.phong.zenflow.notification.dto.UpdateNotificationRequest;
import org.phong.zenflow.notification.infrastructure.persistence.entity.Notification;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface NotificationMapper {
    @Mapping(source = "user.id", target = "userId")
    @Mapping(source = "workflow.id", target = "workflowId")
    NotificationDto toDto(Notification notification);

    @Mapping(source = "userId", target = "user.id")
    @Mapping(source = "workflowId", target = "workflow.id")
    Notification toEntity(CreateNotificationRequest createNotificationRequest);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    Notification partialUpdate(UpdateNotificationRequest updateNotificationRequest, @MappingTarget Notification notification);
}
