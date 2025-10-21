package org.phong.zenflow.workflow.subdomain.context.resolution;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Expiry;
import com.github.benmanes.caffeine.cache.RemovalListener;
import com.github.benmanes.caffeine.cache.Scheduler;
import lombok.NonNull;
import org.phong.zenflow.core.utils.ObjectConversion;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContextKey;
import org.phong.zenflow.workflow.subdomain.context.RuntimeContext;
import org.phong.zenflow.workflow.subdomain.evaluator.services.TemplateService;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.config.WorkflowConfig;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Centralized resolver for configuration/context lookups with adaptive cost-aware caching.
 * <p>
 * Caching is enabled for template resolution when initial refs > 5 or (refs >= 2 and cost reaches expensive threshold).
 * Uses dynamic TTL based on observed resolution cost and reference count to optimize cache retention.
 * <p>
 * <b>Reference Tracking:</b> Leverages pre-computed consumer counts from {@link RuntimeContext} which are
 * calculated during workflow analysis. This eliminates redundant tracking and directly reflects how many
 * downstream nodes will consume each value.
 * <p>
 * <b>Dynamic TTL Formula:</b>
 * <ul>
 *   <li>If refs > 0 (pinned): TTL = clamp(cost × 20, 1s, 15min)</li>
 *   <li>If refs = 0 (idle): TTL = clamp(cost × 20, 1s, 5min)</li>
 *   <li>Under memory pressure ≥ 90%: TTL = 1s (emergency mode)</li>
 * </ul>
 */
@Component
public class ContextValueResolver {

    // TTL Configuration
    private static final Duration MIN_TTL = Duration.ofSeconds(1);
    private static final Duration MAX_TTL_PINNED = Duration.ofMinutes(15);
    private static final double TTL_FACTOR = 20.0; // K: "how many times the cost" to retain
    
    // Cost tracking for template resolution
    private static final double ALPHA = 0.2; // EMA smoothing factor
    private static final long EXPENSIVE_THRESHOLD_NANOS = Duration.ofMillis(10).toNanos(); // Consider expensive if > 10ms

    private final Cache<@NonNull CacheKey, Tracked<Object>> cache;
    private final ConcurrentMap<UUID, ConcurrentMap<String, Set<CacheKey>>> indexByWorkflow = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, CostStats> costByKey = new ConcurrentHashMap<>();
    private final SystemLoadMonitor systemLoadMonitor;

    public ContextValueResolver(SystemLoadMonitor systemLoadMonitor) {
        this.systemLoadMonitor = systemLoadMonitor;
        RemovalListener<@NonNull CacheKey, Tracked<Object>> listener = (key, value, cause) -> {
            if (key != null) {
                unregisterKey(key);
            }
        };
        this.cache = Caffeine.newBuilder()
                .maximumSize(500)
                .scheduler(Scheduler.systemScheduler())
                .expireAfter(new Expiry<CacheKey, Tracked<Object>>() {
                    @Override
                    public long expireAfterCreate(@NonNull CacheKey key, @NonNull Tracked<Object> value, long currentTime) {
                        return calculateTtlNanos(value);
                    }

                    @Override
                    public long expireAfterUpdate(@NonNull CacheKey key, @NonNull Tracked<Object> value, long currentTime, long currentDuration) {
                        return calculateTtlNanos(value);
                    }

                    @Override
                    public long expireAfterRead(@NonNull CacheKey key, @NonNull Tracked<Object> value, long currentTime, long currentDuration) {
                        return calculateTtlNanos(value);
                    }
                })
                .removalListener(listener)
                .recordStats()
                .build();
    }

    /**
     * Calculate dynamic TTL based on cost, refs, and memory pressure.
     * If refs reach 0, return 0 to immediately evict (no consumers left).
     */
    private long calculateTtlNanos(Tracked<Object> tracked) {
        int refs = tracked.refs().get();
        long cost = tracked.emaCostNanos().get();
        
        // If no consumers left, evict immediately
        if (refs <= 0) {
            return 0L;
        }
        
        // Memory pressure guardrail
        double memoryPressure = systemLoadMonitor.readMemoryPressure();
        if (memoryPressure >= 0.90) {
            return MIN_TTL.toNanos();
        }
        
        // Calculate base TTL from cost
        long baseTtl = (cost == 0L) 
            ? MIN_TTL.toNanos() 
            : (long) (cost * TTL_FACTOR);
        
        // Apply cap based on pinning status (refs > 0 since we already handled refs <= 0)
        return clampNanos(baseTtl, MIN_TTL.toNanos(), MAX_TTL_PINNED.toNanos());
    }

    private static long clampNanos(long value, long min, long max) {
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Resolve the value associated with {@code key} from the provided sources. Config values take priority,
     * and are cached according to consumer count and system load. Runtime values act as the fallback.
     */
    public Object resolve(UUID workflowRunId,
                          String nodeKey,
                          String key,
                          WorkflowConfig currentConfig,
                          RuntimeContext runtimeContext,
                          TemplateService templateService,
                          ExecutionContext executionContext) {
        if (templateService != null && templateService.isTemplate(key)) {
            return templateService.resolve(key, executionContext);
        }

        ConfigLookup lookup = lookupConfig(nodeKey, key, currentConfig);
        Object rawConfigValue = lookup.rawValue();
        String normalizedKey = lookup.normalizedKey();

        int consumerCount = determineConsumerCount(runtimeContext, key, normalizedKey);
        boolean shouldCache = shouldCache(consumerCount, normalizedKey);

        if (rawConfigValue != null) {
            Object resolved = shouldCache && workflowRunId != null
                    ? resolveWithCache(workflowRunId, normalizedKey, key, rawConfigValue, templateService, executionContext, runtimeContext, consumerCount)
                    : resolveValue(rawConfigValue, templateService, executionContext);

            maybeInvalidateIfNoConsumers(runtimeContext, workflowRunId, normalizedKey, key);
            return resolved;
        }

        Object runtimeValue = resolveRuntimeValue(nodeKey, key, runtimeContext, templateService, executionContext);
        maybeInvalidateIfNoConsumers(runtimeContext, workflowRunId, normalizedKey, key);
        return runtimeValue;
    }

    /**
     * Indicates whether a configuration entry exists for the provided key.
     */
    public boolean hasConfigValue(String nodeKey, String key, WorkflowConfig currentConfig) {
        return lookupConfig(nodeKey, key, currentConfig).rawValue() != null;
    }

    /**
     * Clears cached values associated with the given workflow once it completes.
     */
    public void invalidateWorkflow(UUID workflowRunId) {
        if (workflowRunId == null) {
            return;
        }
        ConcurrentMap<String, Set<CacheKey>> perWorkflow = indexByWorkflow.remove(workflowRunId);
        if (perWorkflow == null) {
            return;
        }
        perWorkflow.values().stream()
                .flatMap(Collection::stream)
                .forEach(cache::invalidate);
    }

    /**
     * Clears cached entries when the runtime context indicates the value no longer has consumers.
     */
    public void invalidateIfConsumersDepleted(UUID workflowRunId,
                                              String nodeKey,
                                              String key,
                                              RuntimeContext runtimeContext) {
        if (runtimeContext == null || workflowRunId == null) {
            return;
        }
        ConfigLookup lookup = lookupConfig(nodeKey, key, null);
        String normalizedKey = lookup.normalizedKey();
        maybeInvalidateIfNoConsumers(runtimeContext, workflowRunId, normalizedKey, key);
    }

    private Object resolveRuntimeValue(String nodeKey,
                                       String key,
                                       RuntimeContext runtimeContext,
                                       TemplateService templateService,
                                       ExecutionContext executionContext) {
        if (runtimeContext == null) {
            return null;
        }
        Object raw = runtimeContext.getAndClean(nodeKey, key);
        return resolveValue(raw, templateService, executionContext);
    }

    private Object resolveWithCache(UUID workflowRunId,
                                    String normalizedKey,
                                    String originalKey,
                                    Object rawValue,
                                    TemplateService templateService,
                                    ExecutionContext executionContext,
                                    RuntimeContext runtimeContext,
                                    int initialConsumerCount) {
        CacheKey cacheKey = new CacheKey(workflowRunId, normalizedKey, originalKey, rawValue);
        Tracked<Object> tracked = cache.getIfPresent(cacheKey);
        
        if (tracked != null) {
            // Cache hit - update last used time
            tracked.lastUsedNanos().set(System.nanoTime());
            return tracked.resource();
        }
        
        // Cache miss - resolve with profiling
        long t0 = System.nanoTime();
        Object resolved = resolveValue(rawValue, templateService, executionContext);
        long elapsed = System.nanoTime() - t0;
        
        if (resolved != null) {
            // Create tracked entry with cost and live reference to RuntimeContext.
            // The refs() method will query RuntimeContext.getConsumerCount() on each TTL calculation,
            // ensuring the cache reflects the current number of consumers in real-time.
            // When refs reach 0 (no consumers left), TTL becomes 0 and entry is evicted immediately.
            Tracked<Object> newTracked = new Tracked<>(
                    resolved,
                    runtimeContext,      // Live reference for querying consumer count
                    normalizedKey,       // Key to query
                    originalKey,         // Alternative key to query
                    new AtomicLong(0L),  // Cost will be recorded next
                    new AtomicLong(System.nanoTime())
            );
            newTracked.recordCost(elapsed);
            
            // Record cost stats for admission decision
            if (normalizedKey != null) {
                costByKey.computeIfAbsent(normalizedKey, k -> new CostStats())
                        .record(elapsed);
            }
            
            cache.put(cacheKey, newTracked);
            indexKey(cacheKey);
        }
        return resolved;
    }

    private Object resolveValue(Object value,
                                TemplateService templateService,
                                ExecutionContext executionContext) {
        if (value == null) {
            return null;
        }
        if (value instanceof String str) {
            if (templateService != null && templateService.isTemplate(str)) {
                return templateService.resolve(str, executionContext);
            }
            return str;
        }
        if (value instanceof Map<?, ?> map) {
            return resolveMap(ObjectConversion.convertObjectToMap(map), templateService, executionContext);
        }
        if (value instanceof List<?> list) {
            return resolveList(list, templateService, executionContext);
        }
        return value;
    }

    private Map<String, Object> resolveMap(Map<String, Object> input,
                                           TemplateService templateService,
                                           ExecutionContext executionContext) {
        Map<String, Object> resolved = new ConcurrentHashMap<>();
        input.forEach((k, v) -> resolved.put(k, resolveValue(v, templateService, executionContext)));
        return resolved;
    }

    private List<Object> resolveList(List<?> list,
                                     TemplateService templateService,
                                     ExecutionContext executionContext) {
        return list.stream()
                .map(item -> resolveValue(item, templateService, executionContext))
                .toList();
    }

    private ConfigLookup lookupConfig(String nodeKey, String key, WorkflowConfig config) {
        if (key == null || key.isBlank() || config == null) {
            return new ConfigLookup(normalizeKey(nodeKey, key), null);
        }

        String normalizedKey = normalizeKey(nodeKey, key);
        if (normalizedKey == null || normalizedKey.isBlank()) {
            return new ConfigLookup(normalizedKey, null);
        }

        if ("profile".equals(normalizedKey) || "profileKeys".equals(normalizedKey)) {
            return new ConfigLookup(normalizedKey, config.profile());
        }
        if ("output".equals(normalizedKey)) {
            return new ConfigLookup(normalizedKey, config.output());
        }

        Map<String, Object> input = config.input();
        if (input == null || input.isEmpty()) {
            return new ConfigLookup(normalizedKey, null);
        }
        Object value = extractValue(input, normalizedKey);
        return new ConfigLookup(normalizedKey, value);
    }

    private Object extractValue(Map<String, Object> source, String path) {
        if (source == null || path == null) {
            return null;
        }
        if (!path.contains(".")) {
            return source.get(path);
        }
        String[] parts = path.split("\\.");
        Object current = source;
        for (String part : parts) {
            if (current instanceof Map<?, ?> currentMap) {
                current = currentMap.get(part);
            } else {
                return null;
            }
            if (current == null) {
                return null;
            }
        }
        return current;
    }

    private String normalizeKey(String nodeKey, String key) {
        if (key == null) {
            return null;
        }
        String normalized = key.trim();
        if (normalized.isEmpty()) {
            return normalized;
        }
        if (normalized.startsWith(ExecutionContextKey.PROHIBITED_KEY_PREFIX.key())) {
            return normalized;
        }
        if (nodeKey != null && !nodeKey.isBlank()) {
            String prefix = nodeKey + ".";
            if (normalized.startsWith(prefix)) {
                normalized = normalized.substring(prefix.length());
            }
        }
        normalized = stripPrefix(normalized, "config.input.");
        normalized = stripPrefix(normalized, "config.");
        normalized = stripPrefix(normalized, "input.");
        return normalized;
    }

    private String stripPrefix(String value, String prefix) {
        if (value == null || prefix == null) {
            return value;
        }
        return value.startsWith(prefix) ? value.substring(prefix.length()) : value;
    }

    private int determineConsumerCount(RuntimeContext runtimeContext, String originalKey, String normalizedKey) {
        if (runtimeContext == null) {
            return 0;
        }
        int count = 0;
        if (normalizedKey != null) {
            count = Math.max(count, runtimeContext.getConsumerCount(normalizedKey));
        }
        if (originalKey != null) {
            count = Math.max(count, runtimeContext.getConsumerCount(originalKey));
        }
        return count;
    }

    /**
     * Determines if value should be cached based on consumer count and estimated cost.
     * Cache if refs > 5 OR (refs >= 2 AND cost is expensive)
     */
    private boolean shouldCache(int consumerCount, String normalizedKey) {
        // Always cache if many consumers
        if (consumerCount > 5) {
            return true;
        }
        
        // For refs >= 2, check if historically expensive
        if (consumerCount >= 2) {
            CostStats stats = costByKey.get(normalizedKey);
            if (stats != null) {
                long avgCost = stats.getAverageCostNanos();
                return avgCost >= EXPENSIVE_THRESHOLD_NANOS;
            }
            // No history yet, cache it to start tracking
            return true;
        }
        
        return false;
    }

    private void maybeInvalidateIfNoConsumers(RuntimeContext runtimeContext,
                                              UUID workflowRunId,
                                              String normalizedKey,
                                              String originalKey) {
        if (runtimeContext == null || workflowRunId == null) {
            return;
        }
        boolean normalizedEmpty = normalizedKey == null || runtimeContext.isConsumersEmpty(normalizedKey);
        boolean originalEmpty = originalKey == null || runtimeContext.isConsumersEmpty(originalKey);
        if (normalizedEmpty && originalEmpty) {
            invalidateKey(workflowRunId, normalizedKey);
            invalidateKey(workflowRunId, originalKey);
        }
    }

    private void invalidateKey(UUID workflowRunId, String key) {
        if (key == null) {
            return;
        }
        ConcurrentMap<String, Set<CacheKey>> perWorkflow = indexByWorkflow.get(workflowRunId);
        if (perWorkflow == null) {
            return;
        }
        Set<CacheKey> cacheKeys = perWorkflow.remove(key);
        if (cacheKeys != null) {
            cacheKeys.forEach(cache::invalidate);
        }
        if (perWorkflow.isEmpty()) {
            indexByWorkflow.remove(workflowRunId, perWorkflow);
        }
    }

    private void indexKey(CacheKey cacheKey) {
        ConcurrentMap<String, Set<CacheKey>> perWorkflow =
                indexByWorkflow.computeIfAbsent(cacheKey.workflowRunId(), ignored -> new ConcurrentHashMap<>());
        registerKey(perWorkflow, cacheKey.normalizedKey(), cacheKey);
        registerKey(perWorkflow, cacheKey.originalKey(), cacheKey);
    }

    private void registerKey(ConcurrentMap<String, Set<CacheKey>> perWorkflow,
                             String key,
                             CacheKey cacheKey) {
        if (key == null) {
            return;
        }
        perWorkflow.computeIfAbsent(key, ignored -> ConcurrentHashMap.newKeySet())
                .add(cacheKey);
    }

    private void unregisterKey(CacheKey cacheKey) {
        ConcurrentMap<String, Set<CacheKey>> perWorkflow = indexByWorkflow.get(cacheKey.workflowRunId());
        if (perWorkflow == null) {
            return;
        }
        removeKeyFromIndex(perWorkflow, cacheKey.normalizedKey(), cacheKey);
        removeKeyFromIndex(perWorkflow, cacheKey.originalKey(), cacheKey);
        if (perWorkflow.isEmpty()) {
            indexByWorkflow.remove(cacheKey.workflowRunId(), perWorkflow);
        }
    }

    private void removeKeyFromIndex(ConcurrentMap<String, Set<CacheKey>> perWorkflow,
                                    String key,
                                    CacheKey cacheKey) {
        if (key == null) {
            return;
        }
        Set<CacheKey> entries = perWorkflow.get(key);
        if (entries == null) {
            return;
        }
        entries.remove(cacheKey);
        if (entries.isEmpty()) {
            perWorkflow.remove(key, entries);
        }
    }

    private record ConfigLookup(String normalizedKey, Object rawValue) { }

    /**
     * Wrapper that tracks cost and live reference count for cached resources.
     * References are queried live from RuntimeContext to ensure accurate consumer tracking.
     */
    record Tracked<T>(
            T resource,
            RuntimeContext runtimeContext,  // Live reference to RuntimeContext
            String normalizedKey,           // Key to query consumer count
            String originalKey,             // Alternative key to query consumer count
            AtomicLong emaCostNanos,        // exponential moving average of compute time
            AtomicLong lastUsedNanos        // diagnostics/observability
    ) {
        /**
         * Get live reference count from RuntimeContext.
         * Returns the maximum consumer count between normalized and original keys.
         */
        AtomicInteger refs() {
            if (runtimeContext == null) {
                return new AtomicInteger(0);
            }
            int count = Math.max(
                normalizedKey != null ? runtimeContext.getConsumerCount(normalizedKey) : 0,
                originalKey != null ? runtimeContext.getConsumerCount(originalKey) : 0
            );
            return new AtomicInteger(count);
        }
        
        void recordCost(long nanos) {
            long prev = emaCostNanos.get();
            long next = (prev == 0L) ? nanos : (long) (ALPHA * nanos + (1 - ALPHA) * prev);
            emaCostNanos.set(next);
            lastUsedNanos.set(System.nanoTime());
        }
    }

    /**
     * Tracks resolution cost statistics for profiling
     */
    static class CostStats {
        private final AtomicLong totalCostNanos = new AtomicLong(0L);
        private final AtomicInteger count = new AtomicInteger(0);

        void record(long nanos) {
            totalCostNanos.addAndGet(nanos);
            count.incrementAndGet();
        }

        long getAverageCostNanos() {
            int n = count.get();
            return n == 0 ? 0L : totalCostNanos.get() / n;
        }
    }

    private static final class CacheKey {
        private final UUID workflowRunId;
        private final String normalizedKey;
        private final String originalKey;
        private final Object rawValue;

        CacheKey(UUID workflowRunId, String normalizedKey, String originalKey, Object rawValue) {
            this.workflowRunId = workflowRunId;
            this.normalizedKey = normalizedKey != null ? normalizedKey : originalKey;
            this.originalKey = originalKey;
            this.rawValue = rawValue;
        }

        UUID workflowRunId() {
            return workflowRunId;
        }

        String normalizedKey() {
            return normalizedKey;
        }

        String originalKey() {
            return originalKey;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof CacheKey cacheKey)) return false;
            return Objects.equals(workflowRunId, cacheKey.workflowRunId)
                    && Objects.equals(normalizedKey, cacheKey.normalizedKey)
                    && Objects.equals(originalKey, cacheKey.originalKey)
                    && Objects.equals(rawValue, cacheKey.rawValue);
        }

        @Override
        public int hashCode() {
            return Objects.hash(workflowRunId, normalizedKey, originalKey, rawValue);
        }
    }
}
