package org.phong.zenflow.plugin.subdomain.nodes.builtin.core.test.number_validator;

import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.node.definition.aspect.NodeExecutor;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.logging.core.NodeLogPublisher;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class NumberValidatorExecutor implements NodeExecutor {
    @Override
    public ExecutionResult execute(ExecutionContext context) {
        NodeLogPublisher logCollector = context.getLogPublisher();
        
        Integer number = context.read("number", Integer.class);
        Integer threshold = context.read("threshold", Integer.class);
        
        logCollector.info("Number Validator started with number: {}, threshold: {}", number, threshold);
        
        // Mock validation logic
        boolean isValid = number != null && threshold != null && number <= threshold;
        
        Map<String, Object> output = Map.of(
            "valid", isValid,
            "value", number != null ? number : 0,
            "validation_message", isValid ? 
                String.format("Number %d is within acceptable range", number) :
                String.format("Number %d exceeds threshold %d", number, threshold)
        );
        
        logCollector.info("Number validation completed. Valid: {}, Message: {}", 
                        output.get("valid"), output.get("validation_message"));
        
        return ExecutionResult.success(output);
    }
}
