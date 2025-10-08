package org.phong.zenflow.plugin.subdomain.execution.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ExecutionError {
    INTEGRATION_AUTH("Integration authentication error"),
    CANCELLED("Execution was cancelled"),
    TIMEOUT("Execution timed out"),
    RETRIABLE("Retriable error occurred"),
    NON_RETRIABLE("Non-retriable error occurred"),
    NETWORK_TIMEOUT("Network timeout occurred"),
    INTERRUPTED("Execution was interrupted");

    private final String message;
}
