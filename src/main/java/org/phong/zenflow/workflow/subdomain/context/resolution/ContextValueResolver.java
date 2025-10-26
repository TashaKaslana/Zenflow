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
import java.util.concurrent.atomic.AtomicLong;

/**
 * Centralized resolver for configuration/context lookups with adaptive cost-aware caching.
 * <p>
 * <b>Caching Strategy:</b> Only caches expensive <b>template resolution</b> (e.g., {@code {{node.output}}}),
 * not plain config lookups which are already fast. Caching activates when:
 * <ul>
 *   <li>refs > 5 (always cache high-use templates), OR</li>
 *   <li>refs ≥ 2 AND average cost ≥ 10 ms (cache expensive templates)</li>
 * </ul>
 * <p>
 * <b>Reference Tracking:</b> Leverages pre-computed consumer counts from {@link RuntimeContext} which are
 * calculated during workflow analysis. The {@link Tracked#refs()} method queries RuntimeContext live,
 * ensuring cache TTL reflects current consumer count in real-time.
 * <p>
 * <b>Dynamic TTL Formula:</b>
 * <ul>
 *   <li>If refs == 0: TTL = 0 (immediate eviction - no consumers left)</li>
 *   <li>If refs > 0: TTL = clamp (cost × 20, 1 s, 15 min)</li>
 *   <li>Under memory pressure ≥ 90%: TTL = 1 s (emergency mode)</li>
 * </ul>
 */
@Component
public class ContextValueResolver {

    // TTL Configuration
    private static final Duration MIN_TTL = Duration.ofSeconds(1);
    private static final Duration MAX_TTL_PINNED = Duration.ofMinutes(15);
    private static final double TTL_FACTOR = 20.0; // K: "how many times the cost" to retain

    // Cost tracking for template resolution
    private static final long EXPENSIVE_THRESHOLD_NANOS = Duration.ofMillis(10).toNanos(); // Consider expensive if > 10 ms

    private final Cache<@NonNull CacheKey, Tracked<Object>> cache;
    private final ConcurrentMap<UUID, ConcurrentMap<String, Set<CacheKey>>> indexByWorkflow = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, CostStats> costByKey = new ConcurrentHashMap<>();
    private final SystemLoadMonitor systemLoadMonitor;

    public ContextValueResolver(SystemLoadMonitor systemLoadMonitor) {
        this.systemLoadMonitor = systemLoadMonitor;
        RemovalListener<@NonNull CacheKey, @NonNull Tracked<Object>> listener = (key, value, cause) -> {
            if (key != null) {
                unregisterKey(key);
            }
        };
        this.cache = Caffeine.newBuilder()
                .maximumSize(500)
                .scheduler(Scheduler.systemScheduler())
                .expireAfter(new Expiry<@NonNull CacheKey, @NonNull Tracked<Object>>() {
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

    private static long clampNanos(long value, long min, long max) {
        return Math.max(min, Math.min(max, value));
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

    /**
     * Resolve the value associated with {@code key} from the provided sources. Config values take priority
     * and are cached according to consumer count and cost. Runtime values act as the fallback.
     * Only template resolution is cached since it's expensive; plain config lookups are fast and not cached.
     */
    public Object resolve(UUID workflowRunId,
                          String nodeKey,
                          String key,
                          WorkflowConfig currentConfig,
                          RuntimeContext runtimeContext,
                          TemplateService templateService,
                          ExecutionContext executionContext) {
        // Config lookup - cache if the value contains templates and meets criteria
        ConfigLookup lookup = lookupConfig(nodeKey, key, currentConfig);
        Object rawConfigValue = lookup.rawValue();
        String normalizedKey = lookup.normalizedKey();

        if (rawConfigValue == null) {
            Object runtimeValue = resolveRuntimeValue(nodeKey, key, runtimeContext, templateService, executionContext);
            maybeInvalidateIfNoConsumers(runtimeContext, workflowRunId, normalizedKey, key);
            return runtimeValue;
        }

        if (templateService.isTemplate(rawConfigValue.toString()) && workflowRunId != null) {

            int consumerCount = determineConsumerCount(runtimeContext, key, normalizedKey);
            boolean shouldCache = shouldCache(consumerCount, normalizedKey);

            if (shouldCache) {
                return resolveConfigValueWithCache(workflowRunId, normalizedKey, key, rawConfigValue,
                        templateService, executionContext, runtimeContext);
            }

            Object resolved = resolveValue(rawConfigValue, templateService, executionContext);
            maybeInvalidateIfNoConsumers(runtimeContext, workflowRunId, normalizedKey, key);
            return resolved;
        } else {
            return rawConfigValue;
        }
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

    /**
     * Resolve a config value that contains templates with cost-aware caching.
     * Handles both pure templates and composite strings with embedded templates.
     */
    private Object resolveConfigValueWithCache(UUID workflowRunId,
                                               String normalizedKey,
                                               String originalKey,
                                               Object rawValue,
                                               TemplateService templateService,
                                               ExecutionContext executionContext,
                                               RuntimeContext runtimeContext) {
        CacheKey cacheKey = new CacheKey(workflowRunId, normalizedKey, originalKey, rawValue);
        Tracked<Object> tracked = cache.getIfPresent(cacheKey);

        if (tracked != null) {
            // Cache hit - return cached resolved value
            tracked.lastUsedNanos().set(System.nanoTime());
            return tracked.resource();
        }

        // Cache miss - resolve with profiling
        long t0 = System.nanoTime();
        Object resolved = resolveValue(rawValue, templateService, executionContext);
        long elapsed = System.nanoTime() - t0;

        if (resolved != null) {
            Tracked<Object> newTracked = new Tracked<>(
                    resolved,
                    runtimeContext,
                    normalizedKey,
                    originalKey,
                    new AtomicLong(0L),
                    new AtomicLong(System.nanoTime())
            );
            newTracked.recordCost(elapsed);

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
        switch (value) {
            case null -> {
                return null;
            }
            case String str -> {
                if (templateService != null && templateService.isTemplate(str)) {
                    return templateService.resolve(str, executionContext);
                }
                return str;
            }
            case Map<?, ?> map -> {
                return resolveMap(ObjectConversion.convertObjectToMap(map), templateService, executionContext);
            }
            case List<?> list -> {
                return resolveList(list, templateService, executionContext);
            }
            default -> {
            }
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

    private record ConfigLookup(String normalizedKey, Object rawValue) {
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
