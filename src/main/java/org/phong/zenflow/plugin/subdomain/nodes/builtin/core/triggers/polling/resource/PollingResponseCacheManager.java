package org.phong.zenflow.plugin.subdomain.nodes.builtin.core.triggers.polling.resource;

import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.subdomain.resource.BaseNodeResourceManager;
import org.phong.zenflow.plugin.subdomain.resource.trigger.BaseTriggerResourceManager;
import org.phong.zenflow.workflow.subdomain.trigger.resource.DefaultResourceConfig;
import org.springframework.stereotype.Component;

/**
 * Resource manager for polling trigger response caching.
 * Uses the generic {@link BaseNodeResourceManager} pattern for consistent resource management.
 */
@Slf4j
@Component
public class PollingResponseCacheManager extends BaseTriggerResourceManager<PollingResponseCache, DefaultResourceConfig> {
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
