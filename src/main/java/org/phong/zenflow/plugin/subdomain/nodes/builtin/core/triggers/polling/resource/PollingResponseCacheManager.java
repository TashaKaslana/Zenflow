package org.phong.zenflow.plugin.subdomain.nodes.builtin.core.triggers.polling.resource;

import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.subdomain.resource.BaseNodeResourceManager;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.config.WorkflowConfig;
import org.phong.zenflow.workflow.subdomain.trigger.resource.DefaultResourceConfig;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

/**
 * Resource manager for polling trigger response caching.
 * Uses the generic {@link BaseNodeResourceManager} pattern for consistent resource management.
 */
@Slf4j
@Component
public class PollingResponseCacheManager extends BaseNodeResourceManager<PollingResponseCache, DefaultResourceConfig> {
    private static final String TRIGGER_ID_FIELD = "triggerId";

    @Override
    public DefaultResourceConfig buildConfig(WorkflowConfig cfg, ExecutionContext ctx) {
        String triggerId = resolveTriggerId(cfg, ctx);
        return new DefaultResourceConfig(Map.of(TRIGGER_ID_FIELD, triggerId), TRIGGER_ID_FIELD);
    }

    private String resolveTriggerId(WorkflowConfig cfg, ExecutionContext ctx) {
        if (cfg != null && cfg.input() != null) {
            Object triggerIdValue = cfg.input().get("trigger_id");
            if (triggerIdValue != null) {
                return triggerIdValue.toString();
            }
        }

        if (ctx != null && ctx.getWorkflowId() != null) {
            String nodeKey = Objects.requireNonNullElse(ctx.getNodeKey(), "node");
            return ctx.getWorkflowId() + ":" + nodeKey;
        }

        return "polling:" + System.identityHashCode(this);
    }

    @Override
    protected PollingResponseCache createResource(String resourceKey, DefaultResourceConfig config) {
        log.info("Creating polling response cache for key: {}", resourceKey);
        return new PollingResponseCache();
    }

    @Override
    protected void cleanupResource(PollingResponseCache cache) {
        log.info("Cleaning up polling response cache");
        cache.clear();
    }

    @Override
    protected boolean checkResourceHealth(PollingResponseCache cache) {
        // An empty cache is still healthy; we only report unhealthy if null
        return cache != null;
    }
}
