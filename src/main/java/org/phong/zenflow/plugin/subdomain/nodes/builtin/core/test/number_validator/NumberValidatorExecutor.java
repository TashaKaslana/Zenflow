package org.phong.zenflow.plugin.subdomain.nodes.builtin.core.test.number_validator;

import org.phong.zenflow.core.utils.ObjectConversion;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.node.definition.aspect.NodeExecutor;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.config.WorkflowConfig;
import org.phong.zenflow.workflow.subdomain.logging.core.NodeLogPublisher;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class NumberValidatorExecutor implements NodeExecutor {
    @Override
    public ExecutionResult execute(WorkflowConfig config, ExecutionContext context) {
        NodeLogPublisher logCollector = context.getLogPublisher();
        Map<String, Object> input = ObjectConversion.convertObjectToMap(config.input());
        
        Integer number = (Integer) input.get("number");
        Integer threshold = (Integer) input.get("threshold");
        
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
