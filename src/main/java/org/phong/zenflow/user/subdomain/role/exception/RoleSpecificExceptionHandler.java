package org.phong.zenflow.user.subdomain.role.exception;

import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.core.responses.RestApiResponse;
import org.phong.zenflow.core.utils.HttpRequestUtils;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

/**
 * Exception handler for role subdomain specific exceptions
 */
@Slf4j
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RoleSpecificExceptionHandler {

    @ExceptionHandler(RoleNotFoundException.class)
    public ResponseEntity<RestApiResponse<Void>> handleRoleNotFoundException(RoleNotFoundException ex,
                                                                           WebRequest request) {
        log.debug("Role Not Found: {}", ex.getMessage());
        return RestApiResponse.notFound(HttpRequestUtils.getRequestPath(request), ex.getMessage());
    }

    @ExceptionHandler(PermissionAlreadyAssignedException.class)
    public ResponseEntity<RestApiResponse<Void>> handlePermissionAlreadyAssignedException(PermissionAlreadyAssignedException ex,
                                                                                        WebRequest request) {
        log.debug("Permission Already Assigned: {}", ex.getMessage());
        return RestApiResponse.conflict(HttpRequestUtils.getRequestPath(request), ex.getMessage());
    }

    @ExceptionHandler(PermissionNotAssignedException.class)
    public ResponseEntity<RestApiResponse<Void>> handlePermissionNotAssignedException(PermissionNotAssignedException ex,
                                                                                    WebRequest request) {
        log.debug("Permission Not Assigned: {}", ex.getMessage());
        return RestApiResponse.notFound(HttpRequestUtils.getRequestPath(request), ex.getMessage());
    }

    // Generic role domain exception handler
    @ExceptionHandler(RoleDomainException.class)
    public ResponseEntity<RestApiResponse<Void>> handleRoleDomainException(RoleDomainException ex,
                                                                         WebRequest request) {
        log.debug("Role Domain Exception: {}", ex.getMessage());
        return RestApiResponse.badRequest(HttpRequestUtils.getRequestPath(request), ex.getMessage());
    }
}
