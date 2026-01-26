package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Registry for AI tools with Spring AI @Tool annotation support.
 * <p>
 * Auto-discovers all {@link AiToolProvider} implementations via DI.
 * Models can copy() to create independent registries with custom tools.
 */
@Component
@Slf4j
public class AiToolRegistry {
    
    private final List<Object> toolObjects = new ArrayList<>();
    
    /**
     * Spring auto-injects all AiToolProvider implementations
     */
    public AiToolRegistry(List<AiToolProvider> toolProviders) {
        if (toolProviders != null && !toolProviders.isEmpty()) {
            toolProviders.forEach(this::addTool);
            log.info("AiToolRegistry initialized with {} tool providers: {}", 
                    toolProviders.size(),
                    toolProviders.stream()
                            .map(p -> p.getClass().getSimpleName())
                            .collect(Collectors.joining(", ")));
        } else {
            log.warn("AiToolRegistry initialized with no tool providers - AI will have no tools available");
        }
    }
    
    /**
     * Private constructor for copy() - creates empty registry
     */
    private AiToolRegistry() {
    }
    
    public void addTool(Object toolObject) {
        if (toolObject != null) {
            toolObjects.add(toolObject);
            log.info("Added AI tool object: {}", toolObject.getClass().getSimpleName());
        }
    }
    
    public void addTools(Object... tools) {
        for (Object tool : tools) {
            addTool(tool);
        }
    }
    
    public void addTools(List<Object> tools) {
        for (Object tool : tools) {
            addTool(tool);
        }
    }
    
    /**
     * Returns array for ChatClient.tools() - Spring AI uses varargs
     */
    public Object[] getToolObjects() {
        return toolObjects.toArray();
    }
    
    public List<Object> getToolList() {
        return new ArrayList<>(toolObjects);
    }
    
    /**
     * Creates independent copy - models use this to avoid sharing state
     */
    public AiToolRegistry copy() {
        AiToolRegistry copy = new AiToolRegistry();
        copy.toolObjects.addAll(this.toolObjects);
        log.debug("Created copy of tool registry with {} tools", toolObjects.size());
        return copy;
    }
    
    public void removeTool(Object toolObject) {
        if (toolObjects.remove(toolObject)) {
            log.info("Removed AI tool object: {}", toolObject.getClass().getSimpleName());
        }
    }
    
    public void clear() {
        toolObjects.clear();
        log.info("Cleared all AI tools");
    }
    
    public int size() {
        return toolObjects.size();
    }
    
    public boolean isEmpty() {
        return toolObjects.isEmpty();
    }
    
    public String getSummary() {
        return toolObjects.stream()
                .map(obj -> obj.getClass().getSimpleName())
                .collect(Collectors.joining(", ", "Tools: [", "]"));
    }
}
