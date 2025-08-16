package org.phong.zenflow.plugin.subdomain.nodes.builtin.core.test;

import org.phong.zenflow.core.utils.ObjectConversion;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.interfaces.PluginNodeExecutor;
import org.phong.zenflow.workflow.subdomain.context.RuntimeContext;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto.WorkflowConfig;
import org.phong.zenflow.workflow.subdomain.node_logs.utils.LogCollector;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class DataGeneratorExecutor implements PluginNodeExecutor {
    
    @Override
    public String key() {
        return "core:data.generate:1.0.0";
    }

    @Override
    public ExecutionResult execute(WorkflowConfig config, RuntimeContext context) {
        LogCollector logCollector = new LogCollector();
        
        try {
            Map<String, Object> input = ObjectConversion.convertObjectToMap(config.input());
            
            Integer seed = (Integer) input.get("seed");
            String format = (String) input.get("format");
            
            logCollector.info("Data Generator started with seed: {} and format: {}", seed, format);
            
            // Mock data generation based on seed
            Map<String, Object> output = Map.of(
                    "user_email", "test+tag@very-long-domain-name.example.org",
                    "user_age", 123,
                    "user_active", true,
                    "status", "completed"
            );
            
            logCollector.info("Generated mock user data: email={}, age={}, active={}", 
                            output.get("user_email"), output.get("user_age"), output.get("user_active"));
            
            return ExecutionResult.success(output, logCollector.getLogs());
            
        } catch (Exception e) {
            logCollector.error("Data generation failed: " + e.getMessage());
            return ExecutionResult.error("Data generation failed: " + e.getMessage(), logCollector.getLogs());
        }
    }
}
