package org.phong.zenflow.user.exception;

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
 * Exception handler for user-specific exceptions
 */
@Slf4j
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class UserSpecificExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<RestApiResponse<Void>> handleUserNotFoundException(UserNotFoundException ex,
                                                                           WebRequest request) {
        log.debug("User Not Found: {}", ex.getMessage());
        return RestApiResponse.notFound(HttpRequestUtils.getRequestPath(request), ex.getMessage());
    }

    @ExceptionHandler(UserEmailExistsException.class)
    public ResponseEntity<RestApiResponse<Void>> handleUserEmailExistsException(UserEmailExistsException ex,
                                                                              WebRequest request) {
        log.debug("User Email Already Exists: {}", ex.getMessage());
        return RestApiResponse.conflict(HttpRequestUtils.getRequestPath(request), ex.getMessage());
    }

    @ExceptionHandler(UserUsernameExistsException.class)
    public ResponseEntity<RestApiResponse<Void>> handleUserUsernameExistsException(UserUsernameExistsException ex,
                                                                                 WebRequest request) {
        log.debug("Username Already Exists: {}", ex.getMessage());
        return RestApiResponse.conflict(HttpRequestUtils.getRequestPath(request), ex.getMessage());
    }

    // Generic user domain exception handler
    @ExceptionHandler(UserDomainException.class)
    public ResponseEntity<RestApiResponse<Void>> handleUserDomainException(UserDomainException ex,
                                                                         WebRequest request) {
        log.debug("User Domain Exception: {}", ex.getMessage());
        return RestApiResponse.badRequest(HttpRequestUtils.getRequestPath(request), ex.getMessage());
    }
}
