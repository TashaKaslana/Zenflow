package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.cluster;

import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.node.definition.aspect.NodeExecutor;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.logging.core.NodeLogPublisher;
import org.springframework.stereotype.Component;

/**
 * Placeholder executor for the AI cluster output parser child node.
 * Parsing behaviour is orchestrated by the cluster executor; this node captures configuration only.
 */
@Component
@Slf4j
public class AiClusterOutputParserExecutor implements NodeExecutor {

    @Override
    public ExecutionResult execute(ExecutionContext context) {
        NodeLogPublisher logs = context.getLogPublisher();
        logs.info("AI cluster output parser node executed (configuration-only)");
        return ExecutionResult.success();
    }
}
