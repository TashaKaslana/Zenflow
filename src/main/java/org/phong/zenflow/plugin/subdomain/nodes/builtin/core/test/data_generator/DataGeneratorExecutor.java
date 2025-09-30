package org.phong.zenflow.plugin.subdomain.nodes.builtin.core.test.data_generator;

import org.phong.zenflow.core.utils.ObjectConversion;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.node.definition.aspect.NodeExecutor;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.config.WorkflowConfig;
import org.phong.zenflow.workflow.subdomain.logging.core.NodeLogPublisher;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class DataGeneratorExecutor implements NodeExecutor {
    @Override
    public ExecutionResult execute(WorkflowConfig config, ExecutionContext context) {
        NodeLogPublisher logCollector = context.getLogPublisher();
        
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
            
            return ExecutionResult.success(output);

        } catch (Exception e) {
            logCollector.withException(e).error("Data generation failed: {}", e.getMessage());
            return ExecutionResult.error("Data generation failed: " + e.getMessage());
        }
    }
}
