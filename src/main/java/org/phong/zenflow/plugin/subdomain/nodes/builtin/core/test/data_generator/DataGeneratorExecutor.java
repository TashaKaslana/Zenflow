package org.phong.zenflow.plugin.subdomain.nodes.builtin.core.test.data_generator;

import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.node.definition.aspect.NodeExecutor;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.logging.core.NodeLogPublisher;
import org.springframework.stereotype.Component;

@Component
public class DataGeneratorExecutor implements NodeExecutor {
    @Override
    public ExecutionResult execute(ExecutionContext context) {
        NodeLogPublisher logCollector = context.getLogPublisher();
        
        Integer seed = context.read("seed", Integer.class);
        String format = context.read("format", String.class);
        
        logCollector.info("Data Generator started with seed: {} and format: {}", seed, format);
        
        // Mock data generation based on seed
        String userEmail = "test+tag@very-long-domain-name.example.org";
        int userAge = 123;
        boolean userActive = true;
        String status = "completed";
        
        context.write("user_email", userEmail);
        context.write("user_age", userAge);
        context.write("user_active", userActive);
        context.write("status", status);
        
        logCollector.info("Generated mock user data: email={}, age={}, active={}", 
                        userEmail, userAge, userActive);
        
        return ExecutionResult.success();
    }
}
