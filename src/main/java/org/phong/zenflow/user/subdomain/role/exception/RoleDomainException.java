package org.phong.zenflow.user.subdomain.role.exception;

import lombok.Getter;
import org.phong.zenflow.user.subdomain.role.enums.RoleError;

@Getter
public class RoleDomainException  extends RuntimeException {
    private final RoleError error;

    public RoleDomainException(RoleError error, String details) {
        super(error.formatMessage(details));
        this.error = error;
    }
}