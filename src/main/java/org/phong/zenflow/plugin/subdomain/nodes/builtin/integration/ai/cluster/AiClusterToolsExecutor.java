package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.cluster;

import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.node.definition.aspect.NodeExecutor;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.logging.core.NodeLogPublisher;
import org.springframework.stereotype.Component;

/**
 * Configuration-only executor that allows the AI cluster to materialize a tooling child node.
 * The cluster executor inspects the child configuration at runtime, so this node simply succeeds.
 */
@Component
@Slf4j
public class AiClusterToolsExecutor implements NodeExecutor {

    @Override
    public ExecutionResult execute(ExecutionContext context) {
        NodeLogPublisher logs = context.getLogPublisher();
        logs.info("AI cluster tooling node executed (configuration-only)");
        return ExecutionResult.success();
    }
}
