package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.cluster;

import java.util.List;

/**
 * Resolves and assembles the tool set for a cluster execution.
 * Implementations can mix built-in tools, Spring beans, node-as-tool adapters, and memory tools.
 */
public interface ToolRouter {

    /**
     * Build the tool list for the current execution.
     *
     * @return list of Spring AI tool objects (ToolCallback or @Tool-bearing objects)
     */
    List<Object> resolveTools();

    /**
     * Convenience factory for an empty router.
     */
    static ToolRouter noop() {
        return List::of;
    }
}
