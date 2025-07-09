package org.phong.zenflow.user.subdomain.permission.dtos;

import lombok.Data;

@Data
public class CreatePermissionRequest {
    private String feature;
    private String action;
    private String description;
}
