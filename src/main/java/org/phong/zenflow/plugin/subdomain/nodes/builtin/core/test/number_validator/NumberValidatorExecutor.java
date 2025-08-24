package org.phong.zenflow.plugin.subdomain.nodes.builtin.core.test.number_validator;

import org.phong.zenflow.core.utils.ObjectConversion;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.interfaces.PluginNodeExecutor;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto.WorkflowConfig;
import org.phong.zenflow.workflow.subdomain.logging.core.NodeLogPublisher;
import org.springframework.stereotype.Component;
import org.phong.zenflow.plugin.subdomain.node.registry.PluginNode;

import java.util.Map;

@Component
@PluginNode(
        key = "test:number.validate",
        name = "Number Validator",
        version = "1.0.0"
)
public class NumberValidatorExecutor implements PluginNodeExecutor {
    
    @Override
    public String key() {
        return "test:number.validate:1.0.0";
    }

    @Override
    public ExecutionResult execute(WorkflowConfig config, ExecutionContext context) {
        NodeLogPublisher logCollector = context.getLogPublisher();
        
        try {
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

        } catch (Exception e) {
            logCollector.withException(e).error("Number validation failed: {}", e.getMessage());
            return ExecutionResult.error("Number validation failed: " + e.getMessage());
        }
    }
}
