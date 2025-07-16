package org.phong.zenflow.plugin.subdomain.execution.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.phong.zenflow.plugin.subdomain.execution.enums.ExecutionStatus;
import org.phong.zenflow.workflow.subdomain.node_logs.dto.LogEntry;

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

    public static ExecutionResult pause(List<LogEntry> logs) {
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
}
