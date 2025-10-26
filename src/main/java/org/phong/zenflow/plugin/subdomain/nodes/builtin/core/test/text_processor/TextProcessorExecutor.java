package org.phong.zenflow.plugin.subdomain.nodes.builtin.core.test.text_processor;

import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.node.definition.aspect.NodeExecutor;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.logging.core.NodeLogPublisher;
import org.springframework.stereotype.Component;

@Component
public class TextProcessorExecutor implements NodeExecutor {
    @Override
    public ExecutionResult execute(ExecutionContext context) {
        NodeLogPublisher logCollector = context.getLogPublisher();
        
        String text = context.read("text", String.class);
        Integer count = context.read("count", Integer.class); 
        Boolean flag = context.read("flag", Boolean.class);
        
        logCollector.info("Text Processor started with text: {}, count: {}, flag: {}", text, count, flag);
        
        // Mock text processing
        String result = "Processed text: " + text;
        int processedCount = count != null ? count : 0;
        
        context.write("result", result);
        context.write("processed_count", processedCount);
        
        logCollector.info("Text processing completed. Result length: {}, processed count: {}", 
                        result.length(), processedCount);
        
        return ExecutionResult.success();
    }
}
