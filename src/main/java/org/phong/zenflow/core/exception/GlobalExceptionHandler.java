package org.phong.zenflow.core.exception;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.core.enums.SystemError;
import org.phong.zenflow.core.responses.ApiErrorResponse;
import org.phong.zenflow.core.responses.RestApiResponse;
import org.phong.zenflow.core.utils.HttpRequestUtils;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.lang.NonNull;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final ApplicationEventPublisher eventPublisher;
//    private final AuthService authService;
//
//    private void publishLogEvent(LogSeverity severity, String eventMessage, String source, Throwable throwable, Map<String, Object> additionalContext) {
//        UUID userId = null;
//        try {
//            userId = authService.getUserIdFromContext();
//        } catch (Exception e) {
//            log.trace("Could not retrieve userId for log event: {}", e.getMessage());
//        }
//
//        CreateLogEntryRequest logRequest = new CreateLogEntryRequest();
//        logRequest.setSeverity(severity);
//        logRequest.setMessage(eventMessage + (throwable != null ? " - Exception: " + throwable.getMessage() : ""));
//        logRequest.setSource(source);
//        logRequest.setUserId(userId);
//
//        Map<String, Object> context = new HashMap<>();
//        if (additionalContext != null) {
//            context.putAll(additionalContext);
//        }
//        if (throwable != null) {
//            context.put("exceptionType", throwable.getClass().getName());
//        }
//        logRequest.setContext(context);
//
//        try {
//            eventPublisher.publishEvent(new CreateLogEntryEvent(this, logRequest));
//        } catch (Exception e) {
//            log.error("Failed to publish log event: {}", e.getMessage(), e);
//        }
//    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RestApiResponse<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                              @NonNull WebRequest request) {
        log.warn("Validation Error: {}", ex.getMessage());

        Map<String, String> fieldErrors = new HashMap<>();
        List<String> globalErrors = ex.getBindingResult()
                .getGlobalErrors().stream()
                .map(ObjectError::getDefaultMessage)
                .collect(Collectors.toList());

        ex.getBindingResult().getFieldErrors().forEach(error ->
                fieldErrors.put(error.getField(), error.getDefaultMessage())
        );

        Map<String, Object> logContext = new HashMap<>();
        logContext.put("fieldErrors", fieldErrors);
        logContext.put("globalErrors", globalErrors);
//        publishLogEvent(LogSeverity.WARNING, "Validation failed", HttpRequestUtils.getRequestPath(request), ex, logContext);

        return RestApiResponse.validationError(
                HttpRequestUtils.getRequestPath(request),
                SystemError.VALIDATION_FAILED_MSG.getErrorMessage(),
                fieldErrors,
                globalErrors.isEmpty() ? null : globalErrors
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<RestApiResponse<Void>> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                              @NonNull WebRequest request) {
        log.warn("Malformed JSON request: {}", ex.getMessage());
//        publishLogEvent(LogSeverity.WARNING, "Malformed JSON request", HttpRequestUtils.getRequestPath(request), ex, null);
        return RestApiResponse.badRequest(HttpRequestUtils.getRequestPath(request), SystemError.MALFORMED_REQUEST_MSG.getErrorMessage());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<RestApiResponse<Void>> handleHttpRequestMethodNotSupported(
            HttpRequestMethodNotSupportedException ex,
            @NonNull WebRequest request) {
        String message = SystemError.METHOD_NOT_SUPPORTED_MSG + ". Supported methods: " + ex.getSupportedHttpMethods();
        log.warn("Method Not Supported: {}", ex.getMessage());
//        publishLogEvent(LogSeverity.WARNING, "HTTP method not supported", HttpRequestUtils.getRequestPath(request), ex, Map.of("supportedMethods", String.valueOf(ex.getSupportedHttpMethods())));

        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.setAllow(Objects.requireNonNull(ex.getSupportedHttpMethods()));

        ApiErrorResponse errorResponse = new ApiErrorResponse(
                HttpStatus.METHOD_NOT_ALLOWED.value(),
                message,
                HttpRequestUtils.getRequestPath(request),
                HttpStatus.METHOD_NOT_ALLOWED.getReasonPhrase(),
                null,
                null
        );

        return RestApiResponse.methodNotAllowed(errorResponse, SystemError.METHOD_NOT_SUPPORTED_MSG.toString(), responseHeaders);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<RestApiResponse<Void>> handleMissingServletRequestParameter(
            MissingServletRequestParameterException ex,
            @NonNull WebRequest request) {
        log.warn("Missing Request Parameter: {}", ex.getParameterName());
        String message = SystemError.MISSING_PARAMETER_MSG.getErrorMessage() + ": " + ex.getParameterName() + " (" + ex.getParameterType() + ")";
//        publishLogEvent(LogSeverity.WARNING, "Missing request parameter", HttpRequestUtils.getRequestPath(request), ex, Map.of("parameterName", ex.getParameterName(), "parameterType", ex.getParameterType()));
        ApiErrorResponse apiError = new ApiErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                message,
                HttpRequestUtils.getRequestPath(request),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                null,
                null
        );

        return RestApiResponse.badRequest(apiError, SystemError.MISSING_PARAMETER_MSG.getErrorMessage());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<RestApiResponse<Void>> handleConstraintViolationException(
            ConstraintViolationException ex,
            WebRequest request) {
        log.warn("Constraint Violation: {}", ex.getMessage());

        Map<String, String> constraintErrors = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        violation -> violation.getPropertyPath().toString(),
                        ConstraintViolation::getMessage
                ));
//        publishLogEvent(LogSeverity.WARNING, "Constraint violation", HttpRequestUtils.getRequestPath(request), ex, Map.of("constraintErrors", constraintErrors));
        ApiErrorResponse apiError = new ApiErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                SystemError.VALIDATION_FAILED_MSG,
                HttpRequestUtils.getRequestPath(request),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                constraintErrors,
                null
        );
        return RestApiResponse.badRequest(apiError);
    }

    // New handler for security vulnerabilities
    @ExceptionHandler({SecurityException.class})
    public ResponseEntity<RestApiResponse<Void>> handleSecurityException(
            Exception ex, WebRequest request) {
        log.error("SECURITY VULNERABILITY DETECTED: {}", ex.getMessage(), ex);

//        publishLogEvent(
//                LogSeverity.CRITICAL, // Mark security exceptions as CRITICAL
//                "Security vulnerability detected",
//                HttpRequestUtils.getRequestPath(request),
//                ex,
//                Map.of("securityThreat", "true", "threatTimestamp", System.currentTimeMillis())
//        );

        // Don't reveal details of security issues in the response
        return RestApiResponse.forbidden(HttpRequestUtils.getRequestPath(request),
                "Access denied due to security policy");
    }

    // Updating DataIntegrityViolationException to CRITICAL when it appears to be an injection attack
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<RestApiResponse<Void>> handleDataIntegrityViolationException(
            DataIntegrityViolationException ex, WebRequest request) {
        String rootCauseMessage = ex.getRootCause() != null ? ex.getRootCause().getMessage() : "N/A";

        // Check if this might be an SQL injection attempt
        boolean potentialInjectionAttack = rootCauseMessage.toLowerCase().contains("syntax error") ||
                                          rootCauseMessage.toLowerCase().contains("sql") &&
                                          (rootCauseMessage.contains("'") || rootCauseMessage.contains("--"));

//        LogSeverity severity = potentialInjectionAttack ? LogSeverity.CRITICAL : LogSeverity.WARNING;

        if (potentialInjectionAttack) {
            log.error("POTENTIAL INJECTION ATTACK DETECTED: {}", ex.getMessage(), ex);
        } else {
            log.warn("Data Integrity Violation: {}", ex.getMessage());
        }

        if (ex.getRootCause() != null) {
            log.warn("Root cause: {}", rootCauseMessage);
        }

        Map<String, Object> context = new HashMap<>();
        context.put("rootCause", rootCauseMessage);
        if (potentialInjectionAttack) {
            context.put("securityThreat", "potential-sql-injection");
        }

//        publishLogEvent(severity,
//                        potentialInjectionAttack ? "Potential SQL injection detected" : "Data integrity violation",
//                        HttpRequestUtils.getRequestPath(request),
//                        ex,
//                        context);

        return RestApiResponse.conflict(
                HttpRequestUtils.getRequestPath(request),
                SystemError.DATA_INTEGRITY_VIOLATION.getErrorMessage()
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<RestApiResponse<Void>> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            @NonNull WebRequest request) {
        log.warn("Method Argument Type Mismatch: {}", ex.getMessage());

        String message = SystemError.INVALID_ARGUMENT_TYPE_MSG.getErrorMessage() + ": " + ex.getName()
                + " (" + Objects.requireNonNull(ex.getRequiredType()).getSimpleName() + "). "
                + "Getting value type : " + Objects.requireNonNull(ex.getValue()).getClass().getSimpleName();

        ApiErrorResponse apiError = new ApiErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                message,
                HttpRequestUtils.getRequestPath(request),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                null,
                null
        );

//        publishLogEvent(
//                LogSeverity.WARNING,
//                "Method argument type mismatch",
//                HttpRequestUtils.getRequestPath(request),
//                ex,
//                Map.of(
//                        "argumentName", ex.getName(),
//                        "requiredType", ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "null",
//                        "providedValueType", ex.getValue() != null ? ex.getValue().getClass().getSimpleName() : "null"
//                )
//        );

        return RestApiResponse.badRequest(apiError, SystemError.INVALID_ARGUMENT_TYPE_MSG.getErrorMessage());
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<RestApiResponse<Void>> handleNoHandlerFoundException(
            NoHandlerFoundException ex, WebRequest request) {
        log.debug("No Handler Found: {}", ex.getMessage());

//        publishLogEvent(
//                LogSeverity.WARNING,
//                "No handler found for request",
//                HttpRequestUtils.getRequestPath(request),
//                ex,
//                Map.of(
//                        "httpMethod", ex.getHttpMethod(),
//                        "requestURL", ex.getRequestURL()
//                )
//        );

        return RestApiResponse.notFound(HttpRequestUtils.getRequestPath(request), SystemError.NOT_FOUND_ENDPOINT.getErrorMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<RestApiResponse<Void>> handleAccessDeniedException(
            AccessDeniedException ex, WebRequest request) {
        log.warn("Access Denied: {}", ex.getMessage());

//        publishLogEvent(
//                LogSeverity.WARNING,
//                "Access denied",
//                HttpRequestUtils.getRequestPath(request),
//                ex,
//                null
//        );

        return RestApiResponse.forbidden(HttpRequestUtils.getRequestPath(request), SystemError.ACCESS_DENIED.getErrorMessage());
    }

    @ExceptionHandler({AuthenticationException.class, InsufficientAuthenticationException.class})
    public ResponseEntity<RestApiResponse<Void>> handleAuthenticationException(
            AuthenticationException ex, WebRequest request) {
        log.warn("Authentication Failed: {}", ex.getMessage());

//        publishLogEvent(
//                LogSeverity.WARNING,
//                "Authentication failed",
//                HttpRequestUtils.getRequestPath(request),
//                ex,
//                null
//        );

        return RestApiResponse.unauthorized(HttpRequestUtils.getRequestPath(request), SystemError.AUTHENTICATION_FAILED.getErrorMessage());
    }

    // Update global exception handler to mark certain exceptions as CRITICAL
    @ExceptionHandler(Exception.class)
    public ResponseEntity<RestApiResponse<Void>> handleGlobalException(
            Exception ex, WebRequest request) {
        // Determine if this is a critical exception
        boolean isCriticalException = isSystemCriticalException(ex);
//        LogSeverity severity = isCriticalException ? LogSeverity.CRITICAL : LogSeverity.ERROR;

        if (isCriticalException) {
            log.error("CRITICAL SYSTEM EXCEPTION: {}", ex.getMessage(), ex);
        } else {
            log.error("Unhandled exception occurred: {}", ex.getMessage(), ex);
        }

        Map<String, Object> contextData = new HashMap<>();
        if (isCriticalException) {
            contextData.put("critical", true);
            contextData.put("impactLevel", "system");
        }

//        publishLogEvent(
//                severity,
//                isCriticalException ? "Critical system exception detected" : "Unhandled exception occurred",
//                HttpRequestUtils.getRequestPath(request),
//                ex,
//                contextData
//        );

        return RestApiResponse.internalServerError(HttpRequestUtils.getRequestPath(request),
                SystemError.GENERIC_ERROR_MSG.getErrorMessage());
    }

    // Helper method to determine if an exception is critical
    private boolean isSystemCriticalException(Throwable ex) {
        String exceptionName = ex.getClass().getSimpleName().toLowerCase();
        String message = ex.getMessage() != null ? ex.getMessage().toLowerCase() : "";

        // Check for known critical exceptions that would severely impact system functionality
        return ex instanceof OutOfMemoryError ||
               exceptionName.contains("thread") && (exceptionName.contains("death") || exceptionName.contains("kill")) ||
               exceptionName.contains("deadlock") ||
               exceptionName.contains("memory") ||
               (exceptionName.contains("database") && message.contains("connect")) ||
               message.contains("cannot allocate") ||
               message.contains("disk full") ||
               message.contains("connection refused") ||
               message.contains("too many open files");
    }
}
