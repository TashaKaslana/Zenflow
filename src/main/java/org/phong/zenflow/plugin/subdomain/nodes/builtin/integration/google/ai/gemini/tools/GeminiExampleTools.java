package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.google.ai.gemini.tools;

import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base.AiToolProvider;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class GeminiExampleTools implements AiToolProvider {
    
    @Tool(description = "Convert text to uppercase letters")
    public String toUpperCase(@ToolParam(description = "Text to convert") String text) {
        return text != null ? text.toUpperCase() : "";
    }
    
    @Tool(description = "Reverse the order of characters in a string")
    public String reverseString(@ToolParam(description = "Text to reverse") String text) {
        return text != null ? new StringBuilder(text).reverse().toString() : "";
    }
    
    @Tool(description = "Count the number of words in a text")
    public int countWords(@ToolParam(description = "Text to analyze") String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0;
        }
        return text.trim().split("\\s+").length;
    }
}
