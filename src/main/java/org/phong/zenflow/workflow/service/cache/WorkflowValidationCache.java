package org.phong.zenflow.workflow.service.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.phong.zenflow.workflow.service.dto.WorkflowDefinitionUpdateResult;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Component
public class WorkflowValidationCache {
    private final Cache<UUID, WorkflowDefinitionUpdateResult> cache = Caffeine.newBuilder()
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .maximumSize(1_000)
            .build();

    public WorkflowDefinitionUpdateResult get(UUID workflowId) {
        return cache.getIfPresent(workflowId);
    }

    public void put(UUID workflowId, WorkflowDefinitionUpdateResult result) {
        cache.put(workflowId, result);
    }

    public void invalidate(UUID workflowId) {
        cache.invalidate(workflowId);
    }
}
