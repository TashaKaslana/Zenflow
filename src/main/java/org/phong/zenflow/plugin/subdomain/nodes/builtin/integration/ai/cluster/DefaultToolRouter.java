package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.cluster;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base.AiToolRegistry;
import java.util.List;

/**
 * Default ToolRouter implementation.
 * - Starts with the platform tool registry.
 * - Can be extended later for node-as-tool adapters.
 */
@RequiredArgsConstructor
@Slf4j
public class DefaultToolRouter implements ToolRouter {

    private final AiToolRegistry baseRegistry;
    private final ToolRouterConfig config;

    @Override
    public List<Object> resolveTools() {
        if (config != null && !config.isEnabled()) {
            log.info("Tool routing disabled by configuration");
            return List.of();
        }
        // For now we only return the registry contents; custom bean loading is intentionally omitted
        // to keep the router self-contained and avoid ApplicationContext dependency.
        if (config != null && config.getCustomTools() != null && !config.getCustomTools().isEmpty()) {
            log.warn("custom_tools configured but ApplicationContext loading is disabled; ignoring custom_tools");
        }
        return baseRegistry.getToolList();
    }
}
