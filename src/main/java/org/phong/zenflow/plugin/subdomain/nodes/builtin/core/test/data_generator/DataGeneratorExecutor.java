package org.phong.zenflow.plugin.subdomain.nodes.builtin.core.test.data_generator;

import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.node.definition.aspect.NodeExecutor;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.logging.core.NodeLogPublisher;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class DataGeneratorExecutor implements NodeExecutor {
    @Override
    public ExecutionResult execute(ExecutionContext context) {
        NodeLogPublisher logCollector = context.getLogPublisher();
        
        Integer seed = context.read("seed", Integer.class);
        String format = context.read("format", String.class);
        
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
    }
}
