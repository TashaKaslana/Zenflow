package org.phong.zenflow.core.enums;


import lombok.Getter;

@Getter
public enum SystemError {
    VALIDATION_FAILED_MSG("Validation failed. Please check your input."),
    MALFORMED_REQUEST_MSG("The request is malformed or incorrectly formatted."),
    METHOD_NOT_SUPPORTED_MSG("The HTTP method used is not supported for this endpoint."),
    GENERIC_ERROR_MSG("An unexpected error occurred. Please try again later."),
    MISSING_PARAMETER_MSG("A required parameter is missing in the request."),
    DATA_INTEGRITY_VIOLATION("Data integrity violation. The operation could not be completed."),
    INVALID_ARGUMENT_TYPE_MSG("Invalid argument type provided."),
    AUTHENTICATION_FAILED("Authentication failed. Please check your credentials."),
    ACCESS_DENIED("Access denied. You do not have the required permissions."),
    NOT_FOUND_ENDPOINT("The requested endpoint was not found."),
    ;

    private final String errorMessage;

    SystemError(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
