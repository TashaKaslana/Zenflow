package org.phong.zenflow.plugin.subdomain.execution.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.phong.zenflow.plugin.subdomain.execution.enums.ExecutionStatus;
import org.phong.zenflow.workflow.subdomain.node_logs.dto.LogEntry;
import org.phong.zenflow.workflow.subdomain.schema_validator.dto.ValidationResult;

import java.util.List;
import java.util.Map;

@AllArgsConstructor
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExecutionResult {
    private ExecutionStatus status;
    private List<LogEntry> logs;
    private String error;
    private Map<String, Object> output;
    private String nextNodeKey;
    private ValidationResult validationResult;

    public static ExecutionResult success(Map<String, Object> output, List<LogEntry> logs) {
        ExecutionResult result = new ExecutionResult();
        result.setStatus(ExecutionStatus.SUCCESS);
        result.setOutput(output);
        result.setLogs(logs);
        return result;
    }

    public static ExecutionResult error(String errorMessage, List<LogEntry> logs) {
        ExecutionResult result = new ExecutionResult();
        result.setStatus(ExecutionStatus.ERROR);
        result.setError(errorMessage);
        result.setLogs(logs);
        return result;
    }

    public static ExecutionResult waiting(List<LogEntry> logs) {
        ExecutionResult result = new ExecutionResult();
        result.setStatus(ExecutionStatus.WAITING);
        result.setLogs(logs);
        return result;
    }

    public static ExecutionResult retry(String errorMessage, List<LogEntry> logs) {
        ExecutionResult result = new ExecutionResult();
        result.setStatus(ExecutionStatus.RETRY);
        result.setError(errorMessage);
        result.setLogs(logs);
        return result;
    }

    public static ExecutionResult nextNode(String nextNodeKey) {
        ExecutionResult result = new ExecutionResult();
        result.setStatus(ExecutionStatus.NEXT);
        result.setNextNodeKey(nextNodeKey);
        result.setOutput(null);
        result.setLogs(null);
        return result;
    }

    public static ExecutionResult nextNode(String nextNodeKey, Map<String, Object> output) {
        ExecutionResult result = nextNode(nextNodeKey);
        result.setOutput(output);
        return result;
    }

    public static ExecutionResult nextNode(String nextNodeKey, List<LogEntry> logs) {
        ExecutionResult result = nextNode(nextNodeKey);
        result.setLogs(logs);
        return result;
    }

    public static ExecutionResult nextNode(String nextNodeKey, Map<String, Object> output, List<LogEntry> logs) {
        ExecutionResult result = nextNode(nextNodeKey);
        result.setOutput(output);
        result.setLogs(logs);
        return result;
    }

    public static ExecutionResult validationError(ValidationResult validationResult, String nodeKey) {
        ExecutionResult result = new ExecutionResult();
        result.setStatus(ExecutionStatus.VALIDATION_ERROR);
        result.setValidationResult(validationResult);
        result.setNextNodeKey(nodeKey);
        return result;
    }

    public static ExecutionResult loopNext(String nextNode, Map<String, Object> output, List<LogEntry> logs) {
        ExecutionResult result = new ExecutionResult();
        result.setStatus(ExecutionStatus.LOOP_NEXT);
        result.setNextNodeKey(nextNode);
        result.setOutput(output);
        result.setLogs(logs);
        return result;
    }

    public static ExecutionResult loopEnd(String loopEndNode, Map<String, Object> output, List<LogEntry> logs) {
        ExecutionResult result = new ExecutionResult();
        result.setStatus(ExecutionStatus.LOOP_END);
        result.setNextNodeKey(loopEndNode);
        result.setOutput(output);
        result.setLogs(logs);
        return result;
    }

    public static ExecutionResult loopEnd(String loopEndNode, List<LogEntry> logs) {
        return ExecutionResult.loopEnd(loopEndNode, null, logs);
    }

    public static ExecutionResult loopContinue(Map<String, Object> output, List<LogEntry> logs) {
        ExecutionResult result = new ExecutionResult();
        result.setStatus(ExecutionStatus.LOOP_CONTINUE);
        result.setOutput(output);
        result.setLogs(logs);
        return result;
    }
    public static ExecutionResult loopBreak(String nextNode, Map<String, Object> output, List<LogEntry> logs) {
        ExecutionResult result = new ExecutionResult();
        result.setStatus(ExecutionStatus.LOOP_BREAK);
        result.setNextNodeKey(nextNode);
        result.setOutput(output);
        result.setLogs(logs);
        return result;
    }

    public static ExecutionResult commit(Map<String, Object> output, List<LogEntry> logs) {
        ExecutionResult result = new ExecutionResult();
        result.setStatus(ExecutionStatus.COMMIT);
        result.setOutput(output);
        result.setLogs(logs);
        return result;
    }

    public static ExecutionResult uncommit(Map<String, Object> output, List<LogEntry> logs) {
        ExecutionResult result = new ExecutionResult();
        result.setStatus(ExecutionStatus.UNCOMMIT);
        result.setOutput(output);
        result.setLogs(logs);
        return result;
    }
}
