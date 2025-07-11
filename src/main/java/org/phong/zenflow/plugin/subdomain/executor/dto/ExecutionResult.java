package org.phong.zenflow.plugin.subdomain.executor.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@AllArgsConstructor
@Getter
@Setter
public class ExecutionResult {
    private String status;
    private Map<String, Object> output;
}
