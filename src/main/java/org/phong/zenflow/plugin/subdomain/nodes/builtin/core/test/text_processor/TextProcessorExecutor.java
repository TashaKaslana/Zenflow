package org.phong.zenflow.plugin.subdomain.nodes.builtin.core.test.text_processor;

import org.phong.zenflow.core.utils.ObjectConversion;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.node.definition.aspect.NodeExecutor;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.config.WorkflowConfig;
import org.phong.zenflow.workflow.subdomain.logging.core.NodeLogPublisher;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class TextProcessorExecutor implements NodeExecutor {
    @Override
    public ExecutionResult execute(WorkflowConfig config, ExecutionContext context) {
        NodeLogPublisher logCollector = context.getLogPublisher();
        
        try {
            Map<String, Object> input = ObjectConversion.convertObjectToMap(config.input());
            
            String text = (String) input.get("text");
            Integer count = (Integer) input.get("count"); 
            Boolean flag = (Boolean) input.get("flag");
            
            logCollector.info("Text Processor started with text: {}, count: {}, flag: {}", text, count, flag);
            
            // Mock text processing
            Map<String, Object> output = Map.of(
                "result", "Processed text: " + text,
                "processed_count", count != null ? count : 0
            );
            
            logCollector.info("Text processing completed. Result length: {}, processed count: {}", 
                            output.get("result").toString().length(), output.get("processed_count"));
            
            return ExecutionResult.success(output);

        } catch (Exception e) {
            logCollector.withException(e).error("Text processing failed: {}", e.getMessage());
            return ExecutionResult.error("Text processing failed: " + e.getMessage());
        }
    }
}
