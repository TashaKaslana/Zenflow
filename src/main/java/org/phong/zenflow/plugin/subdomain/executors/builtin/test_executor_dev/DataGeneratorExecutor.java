package org.phong.zenflow.plugin.subdomain.executors.builtin.test_executor_dev;

import org.phong.zenflow.core.utils.ObjectConversion;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.interfaces.PluginNodeExecutor;
import org.phong.zenflow.workflow.subdomain.context.RuntimeContext;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto.WorkflowConfig;
import org.phong.zenflow.workflow.subdomain.node_logs.utils.LogCollector;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Random;
import java.util.UUID;

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
            
            Number seedNumber = (Number) input.get("seed");
            String format = (String) input.get("format");

            logCollector.info("Data Generator started with seed: {} and format: {}", seedNumber, format);

            Random random = seedNumber != null ? new Random(seedNumber.longValue()) : new Random();

            String email = "user" + random.nextInt(100_000) + "@example.com";
            int age = random.nextInt(82) + 18; // age between 18 and 99
            boolean active = random.nextBoolean();

            Map<String, Object> output = Map.of(
                    "user_email", email,
                    "user_age", age,
                    "user_active", active,
                    "status", "completed",
                    "generated_id", UUID.randomUUID().toString()
            );

            logCollector.info("Generated mock user data: email={}, age={}, active={}",
                            email, age, active);
            
            return ExecutionResult.success(output, logCollector.getLogs());
            
        } catch (Exception e) {
            logCollector.error("Data generation failed: " + e.getMessage());
            return ExecutionResult.error("Data generation failed: " + e.getMessage(), logCollector.getLogs());
        }
    }
}
