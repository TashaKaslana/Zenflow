package org.phong.zenflow.plugin.subdomain.execution.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.phong.zenflow.plugin.subdomain.execution.enums.ExecutionStatus;
import org.phong.zenflow.workflow.subdomain.schema_validator.dto.ValidationResult;

import java.util.Map;

@AllArgsConstructor
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExecutionResult {
    private ExecutionStatus status;
    private String error;
    private Map<String, Object> output;
    private String nextNodeKey;
    private ValidationResult validationResult;

    public static ExecutionResult success(Map<String, Object> output) {
        ExecutionResult result = new ExecutionResult();
        result.setStatus(ExecutionStatus.SUCCESS);
        result.setOutput(output);
        return result;
    }

    public static ExecutionResult error(String errorMessage) {
        ExecutionResult result = new ExecutionResult();
        result.setStatus(ExecutionStatus.ERROR);
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

    public static ExecutionResult nextNode(String nextNodeKey) {
        ExecutionResult result = new ExecutionResult();
        result.setStatus(ExecutionStatus.NEXT);
        result.setNextNodeKey(nextNodeKey);
        result.setOutput(null);
        return result;
    }

    public static ExecutionResult nextNode(String nextNodeKey, Map<String, Object> output) {
        ExecutionResult result = nextNode(nextNodeKey);
        result.setOutput(output);
        return result;
    }

    public static ExecutionResult validationError(ValidationResult validationResult, String nodeKey) {
        ExecutionResult result = new ExecutionResult();
        result.setStatus(ExecutionStatus.VALIDATION_ERROR);
        result.setValidationResult(validationResult);
        result.setNextNodeKey(nodeKey);
        return result;
    }

    public static ExecutionResult loopNext(String nextNode, Map<String, Object> output) {
        ExecutionResult result = new ExecutionResult();
        result.setStatus(ExecutionStatus.LOOP_NEXT);
        result.setNextNodeKey(nextNode);
        result.setOutput(output);
        return result;
    }

    public static ExecutionResult loopEnd(String loopEndNode, Map<String, Object> output) {
        ExecutionResult result = new ExecutionResult();
        result.setStatus(ExecutionStatus.LOOP_END);
        result.setNextNodeKey(loopEndNode);
        result.setOutput(output);
        return result;
    }

    public static ExecutionResult loopEnd(String loopEndNode) {
        return ExecutionResult.loopEnd(loopEndNode, null);
    }

    public static ExecutionResult loopContinue(Map<String, Object> output) {
        ExecutionResult result = new ExecutionResult();
        result.setStatus(ExecutionStatus.LOOP_CONTINUE);
        result.setOutput(output);
        return result;
    }
    public static ExecutionResult loopBreak(String nextNode, Map<String, Object> output) {
        ExecutionResult result = new ExecutionResult();
        result.setStatus(ExecutionStatus.LOOP_BREAK);
        result.setNextNodeKey(nextNode);
        result.setOutput(output);
        return result;
    }

    public static ExecutionResult commit(Map<String, Object> output) {
        ExecutionResult result = new ExecutionResult();
        result.setStatus(ExecutionStatus.COMMIT);
        result.setOutput(output);
        return result;
    }

    public static ExecutionResult uncommit(Map<String, Object> output) {
        ExecutionResult result = new ExecutionResult();
        result.setStatus(ExecutionStatus.UNCOMMIT);
        result.setOutput(output);
        return result;
    }
}
