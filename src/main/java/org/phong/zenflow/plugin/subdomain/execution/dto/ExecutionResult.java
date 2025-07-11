package org.phong.zenflow.plugin.subdomain.execution.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@AllArgsConstructor
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExecutionResult {
    private String status;
    private List<String> logs;
    private String error;
    private Map<String, Object> output;

    public static ExecutionResult success(Map<String, Object> output, List<String> logs) {
        ExecutionResult result = new ExecutionResult();
        result.setStatus("success");
        result.setOutput(output);
        result.setLogs(logs);
        return result;
    }

    public static ExecutionResult error(String errorMessage, List<String> logs) {
        ExecutionResult result = new ExecutionResult();
        result.setStatus("error");
        result.setError(errorMessage);
        result.setLogs(logs);
        return result;
    }
}
