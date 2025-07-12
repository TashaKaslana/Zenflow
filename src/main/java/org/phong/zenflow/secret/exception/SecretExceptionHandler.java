package org.phong.zenflow.secret.exception;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.core.responses.ApiErrorResponse;
import org.phong.zenflow.core.responses.RestApiResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@AllArgsConstructor
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecretExceptionHandler {
    @ExceptionHandler(SecretDomainException.class)
    public ResponseEntity<RestApiResponse<Void>> handleSecretDomainException(SecretDomainException ex) {
        log.debug("Secret Domain Exception exception occurred: {}", ex.getMessage());

        ApiErrorResponse errorResponse = new ApiErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Secret Domain Error",
                ex.getMessage()
        );

        return RestApiResponse.badRequest(errorResponse, "An error occurred with the Secret Domain: " + ex.getMessage());
    }
}
