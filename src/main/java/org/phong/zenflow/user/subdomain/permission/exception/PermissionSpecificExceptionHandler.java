package org.phong.zenflow.user.subdomain.permission.exception;

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
 * Exception handler for permission subdomain specific exceptions
 */
@Slf4j
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PermissionSpecificExceptionHandler {

    @ExceptionHandler(PermissionNotFoundException.class)
    public ResponseEntity<RestApiResponse<Void>> handlePermissionNotFoundException(PermissionNotFoundException ex,
                                                                                 WebRequest request) {
        log.debug("Permission Not Found: {}", ex.getMessage());
        return RestApiResponse.notFound(HttpRequestUtils.getRequestPath(request), ex.getMessage());
    }

    @ExceptionHandler(PermissionAlreadyExistsException.class)
    public ResponseEntity<RestApiResponse<Void>> handlePermissionAlreadyExistsException(PermissionAlreadyExistsException ex,
                                                                                      WebRequest request) {
        log.debug("Permission Already Exists: {}", ex.getMessage());
        return RestApiResponse.conflict(HttpRequestUtils.getRequestPath(request), ex.getMessage());
    }

    // Generic permission domain exception handler
    @ExceptionHandler(PermissionDomainException.class)
    public ResponseEntity<RestApiResponse<Void>> handlePermissionDomainException(PermissionDomainException ex,
                                                                               WebRequest request) {
        log.debug("Permission Domain Exception: {}", ex.getMessage());
        return RestApiResponse.badRequest(HttpRequestUtils.getRequestPath(request), ex.getMessage());
    }
}
