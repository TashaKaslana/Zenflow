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
public class TextProcessorExecutor implements PluginNodeExecutor {

    @Override
    public String key() {
        return "core:text.process:1.0.0";
    }

    @Override
    public ExecutionResult execute(WorkflowConfig config, RuntimeContext context) {
        LogCollector logCollector = new LogCollector();
        
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
            
            return ExecutionResult.success(output, logCollector.getLogs());
            
        } catch (Exception e) {
            logCollector.error("Text processing failed: " + e.getMessage());
            return ExecutionResult.error("Text processing failed: " + e.getMessage(), logCollector.getLogs());
        }
    }
}
