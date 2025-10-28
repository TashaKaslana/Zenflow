package org.phong.zenflow.plugin.subdomain.execution.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.phong.zenflow.plugin.subdomain.execution.enums.ExecutionError;
import org.phong.zenflow.plugin.subdomain.execution.enums.ExecutionStatus;
import org.phong.zenflow.workflow.subdomain.schema_validator.dto.ValidationResult;


@AllArgsConstructor
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExecutionResult {
    private ExecutionStatus status;
    private ExecutionError errorType;
    private String errorLabel;
    private String error;
    private String nextNodeKey;
    private ValidationResult validationResult;

    public static ExecutionResult success() {
        ExecutionResult result = new ExecutionResult();
        result.setStatus(ExecutionStatus.SUCCESS);
        return result;
    }

    public static ExecutionResult error(String errorMessage) {
        ExecutionResult result = new ExecutionResult();
        result.setStatus(ExecutionStatus.ERROR);
        result.setError(errorMessage);
        return result;
    }

    public static ExecutionResult error(ExecutionError errorType, String errorLabel, String errorMessage) {
        ExecutionResult result = new ExecutionResult();
        result.setStatus(ExecutionStatus.ERROR);
        result.setErrorType(errorType);
        result.setErrorLabel(errorLabel != null ? errorLabel : errorType.getMessage());
        result.setError(errorMessage);
        return result;
    }

    public static ExecutionResult error(ExecutionError errorType, String errorMessage) {
        ExecutionResult result = new ExecutionResult();
        result.setStatus(ExecutionStatus.ERROR);
        result.setErrorType(errorType);
        result.setErrorLabel(errorType.getMessage());
        result.setError(errorMessage);
        return result;
    }

    public static ExecutionResult waiting() {
        ExecutionResult result = new ExecutionResult();
        result.setStatus(ExecutionStatus.WAITING);
        return result;
    }

    public static ExecutionResult retry(String errorMessage) {
        ExecutionResult result = new ExecutionResult();
        result.setStatus(ExecutionStatus.RETRY);
        result.setError(errorMessage);
        return result;
    }

    public static ExecutionResult retry(String errLabel, String debugInfo) {
        ExecutionResult result = new ExecutionResult();
        result.setStatus(ExecutionStatus.RETRY);
        result.setErrorLabel(errLabel);
        result.setError(debugInfo);
        return result;
    }

    public static ExecutionResult nextNode(String nextNodeKey) {
        ExecutionResult result = new ExecutionResult();
        result.setStatus(ExecutionStatus.NEXT);
        result.setNextNodeKey(nextNodeKey);
        return result;
    }

    public static ExecutionResult validationError(ValidationResult validationResult, String nodeKey) {
        ExecutionResult result = new ExecutionResult();
        result.setStatus(ExecutionStatus.VALIDATION_ERROR);
        result.setValidationResult(validationResult);
        result.setNextNodeKey(nodeKey);
        return result;
    }

    public static ExecutionResult loopNext(String nextNode) {
        ExecutionResult result = new ExecutionResult();
        result.setStatus(ExecutionStatus.LOOP_NEXT);
        result.setNextNodeKey(nextNode);
        return result;
    }

    public static ExecutionResult loopEnd(String loopEndNode) {
        ExecutionResult result = new ExecutionResult();
        result.setStatus(ExecutionStatus.LOOP_END);
        result.setNextNodeKey(loopEndNode);
        return result;
    }

    public static ExecutionResult loopContinue() {
        ExecutionResult result = new ExecutionResult();
        result.setStatus(ExecutionStatus.LOOP_CONTINUE);
        return result;
    }

    public static ExecutionResult loopBreak(String nextNode) {
        ExecutionResult result = new ExecutionResult();
        result.setStatus(ExecutionStatus.LOOP_BREAK);
        result.setNextNodeKey(nextNode);
        return result;
    }   

    public static ExecutionResult commit() {
        ExecutionResult result = new ExecutionResult();
        result.setStatus(ExecutionStatus.COMMIT);
        return result;
    }

    public static ExecutionResult uncommit() {
        ExecutionResult result = new ExecutionResult();
        result.setStatus(ExecutionStatus.UNCOMMIT);
        return result;
    }

    public static ExecutionResult cancelledResult(String message) {
        ExecutionResult result = new ExecutionResult();
        result.setStatus(ExecutionStatus.ERROR);
        result.setErrorType(ExecutionError.CANCELLED);
        result.setError(message);
        result.setErrorLabel(ExecutionError.CANCELLED.getMessage());
        return result;
    }

    public static ExecutionResult interruptedResult(String message) {
        ExecutionResult result = new ExecutionResult();
        result.setStatus(ExecutionStatus.ERROR);
        result.setErrorType(ExecutionError.INTERRUPTED);
        result.setErrorLabel(ExecutionError.INTERRUPTED.getMessage());
        result.setError(message);
        return result;
    }
}
