package org.phong.zenflow.core.responses;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.phong.zenflow.core.enums.SystemError;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@RequiredArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiErrorResponse {
    private final int status;
    private final String message;
    private final String path;
    private LocalDateTime timestamp = LocalDateTime.now();
    private String error;
    private Object fieldErrors; // For validation errors specifically
    private List<String> globalErrors; // For global validation errors

    public ApiErrorResponse(int status, SystemError message, String path, String error) {
        this.status = status;
        this.message = message.getErrorMessage();
        this.path = path;
        this.error = error;
    }

    public ApiErrorResponse(int status, String message, String path, String error) {
        this.status = status;
        this.message = message;
        this.path = path;
        this.error = error;
    }

    public ApiErrorResponse(int status, SystemError message, String path, String error, Map<String, Object> fieldErrors, List<String> globalErrors) {
        this(status, message, path, error);
        this.fieldErrors = fieldErrors;
        this.globalErrors = globalErrors;
    }

    public ApiErrorResponse(int status, String message, String path, String error, Map<String, Object> fieldErrors, List<String> globalErrors) {
        this(status, message, path, error);
        this.fieldErrors = fieldErrors;
        this.globalErrors = globalErrors;
    }

    public ApiErrorResponse(int status, String message, String path, String error, Object fieldErrors, List<String> globalErrors) {
        this(status, message, path, error);
        this.fieldErrors = fieldErrors;
        this.globalErrors = globalErrors;
    }
}