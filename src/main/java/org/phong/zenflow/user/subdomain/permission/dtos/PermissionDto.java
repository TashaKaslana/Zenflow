package org.phong.zenflow.user.subdomain.permission.dtos;

import lombok.Data;

import java.util.UUID;

@Data
public class PermissionDto {
    private UUID id;
    private String feature;
    private String action;
    private String description;
}
