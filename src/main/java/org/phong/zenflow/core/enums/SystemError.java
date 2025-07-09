package org.phong.zenflow.core.enums;


import lombok.Getter;

@Getter
public enum SystemError {
    VALIDATION_FAILED_MSG("system.validation_failed"),
    MALFORMED_REQUEST_MSG("system.malformed_request"),
    METHOD_NOT_SUPPORTED_MSG("system.method_not_supported"),
    GENERIC_ERROR_MSG("system.generic_error"),
    MISSING_PARAMETER_MSG("system.missing_parameter"),
    DATA_INTEGRITY_VIOLATION("system.data_integrity_violation"),
    INVALID_ARGUMENT_TYPE_MSG("system.invalid_argument_type"),
    AUTHENTICATION_FAILED("system.authentication_failed"),
    ACCESS_DENIED("system.access_denied"),
    NOT_FOUND_ENDPOINT("system.not_found_endpoint"),
    ;

    private final String messageKey;

    SystemError(String messageKey) {
        this.messageKey = messageKey;
    }
}
