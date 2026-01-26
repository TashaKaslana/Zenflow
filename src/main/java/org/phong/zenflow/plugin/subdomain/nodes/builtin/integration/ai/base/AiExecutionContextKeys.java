package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base;

/**
 * Shared execution context keys used by AI executors, providers, and orchestrators.
 */
public final class AiExecutionContextKeys {

    /**
     * Key that stores the fully constructed {@link org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base.dto.AiExecutionRequest}
     * on the {@link org.phong.zenflow.workflow.subdomain.context.ExecutionContext} before invoking a provider node.
     */
    public static final String TYPED_REQUEST = "ai.execution.request";

    /**
     * Prefix applied to flattened metadata entries written back to the execution context.
     */
    public static final String METADATA_PREFIX = "metadata.";

    /**
     * Context key reserved for structured usage details (prompt/completion tokens, cost, etc.).
     */
    public static final String USAGE_KEY = "usage";

    private AiExecutionContextKeys() {
    }
}
