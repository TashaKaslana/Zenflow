package org.phong.zenflow.workflow.subdomain.context;

import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.workflow.subdomain.context.common.ContextKeyResolver;
import org.phong.zenflow.workflow.subdomain.context.refvalue.RefValue;
import org.phong.zenflow.workflow.subdomain.context.refvalue.RuntimeContextRefValueSupport;
import org.phong.zenflow.workflow.subdomain.context.refvalue.dto.WriteOptions;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * RuntimeContext manages shared context and consumer relationships for workflow execution.
 * 
 * <p><b>RefValue Integration</b>: Internally uses {@link RefValue} for efficient storage
 * of large payloads (JSON, files, streams) without exhausting heap memory. Legacy APIs
 * transparently convert between Object and RefValue for backward compatibility.
 * 
 * <p>Storage is automatically optimized:
 * <ul>
 *   <li>Small values (< 1MB) → Memory</li>
 *   <li>Medium JSON (1-2MB) → Parsed tree with JsonPointer queries</li>
 *   <li>Large payloads (> 1MB) → Temp files on disk</li>
 * </ul>
 * 
 * @see RefValue
 * @see RuntimeContextRefValueSupport
 */
@Slf4j
public class RuntimeContext {
    
    // Core storage: values are now RefValue instances for efficient memory management
    private final Map<String, RefValue> context = new ConcurrentHashMap<>();
    
    // Consumer tracking and aliases remain unchanged
    private final Map<String, AtomicInteger> consumers = new ConcurrentHashMap<>();
    private final Map<String, String> aliases = new ConcurrentHashMap<>();

    // Loop-aware cleanup management
    private final Map<String, Map<String, Set<String>>> pendingLoopCleanup = new ConcurrentHashMap<>();
    private final Set<String> activeLoops = new HashSet<>();
    
    // Pending writes management for transactional context updates
    private final Map<String, PendingWrite> pendingWrites = new HashMap<>();
    
    private record PendingWrite(Object value, WriteOptions options) {}
    
    private final RuntimeContextRefValueSupport refValueSupport;
    
    /**
     * Constructor with optional RefValue support injection.
     * 
     * @param refValueSupport support layer for RefValue operations (null = create default)
     */
    public RuntimeContext(RuntimeContextRefValueSupport refValueSupport) {
        this.refValueSupport = refValueSupport != null ? refValueSupport : new RuntimeContextRefValueSupport();
    }
    
    /**
     * Default constructor.
     */
    public RuntimeContext() {
        this(null);
    }

    public void initialize(Map<String, Object> initialContext,
                           Map<String, Set<String>> initialConsumers,
                           Map<String, String> initialAliases) {
        if (initialConsumers != null) {
            initialConsumers.forEach((key, value) -> {
                if (value != null) {
                    consumers.put(key, new AtomicInteger(value.size()));
                }
            });
        }
        if (initialContext != null) {
            for (Map.Entry<String, Object> entry : initialContext.entrySet()) {
                context.put(entry.getKey(), refValueSupport.objectToRefValue(entry.getKey(), entry.getValue()));
            }
        }
        if (initialAliases != null) {
            aliases.putAll(
                    initialAliases.entrySet().stream()
                            .filter(e -> e.getValue() != null)
                            .collect(
                                    HashMap::new,
                                    (m, e) -> m.put(e.getKey(), e.getValue()),
                                    HashMap::putAll
                            )
            );
        }
    }

    public void put(String key, Object value) {
        RefValue refValue = refValueSupport.objectToRefValue(key, value);
        context.put(key, refValue);
    }

    public void putAll(Map<String, Object> entries) {
        if (entries != null) {
            for (Map.Entry<String, Object> entry : entries.entrySet()) {
                put(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * Retrieves a value from the context by key.
     * RefValue is automatically materialized to Object for backward compatibility.
     *
     * @param key context key
     * @return the value associated with the key, or null if not present
     */
    public Object get(String key) {
        String resolvedKey = aliases.getOrDefault(key, key);
        RefValue refValue = context.get(resolvedKey);
        return refValueSupport.refValueToObject(resolvedKey, refValue);
    }
    
    /**
     * Retrieves the RefValue directly without materialization.
     * Use this for streaming access to large payloads without loading into memory.
     * 
     * @param key context key
     * @return the RefValue associated with the key, or null if not present
     */
    public RefValue getRef(String key) {
        String resolvedKey = aliases.getOrDefault(key, key);
        return context.get(resolvedKey);
    }
    
    /**
     * Opens a stream to read the value as raw bytes without full materialization.
     * Useful for large binary payloads (files, videos, images) that should be streamed
     * rather than loaded entirely into memory.
     * 
     * <p>The caller is responsible for closing the returned InputStream.
     * 
     * @param key the context key to stream
     * @return InputStream over the value's raw bytes
     * @throws java.io.IOException if the stream cannot be opened or the value doesn't exist
     */
    public java.io.InputStream openStream(String key) throws java.io.IOException {
        String resolvedKey = aliases.getOrDefault(key, key);
        RefValue refValue = context.get(resolvedKey);
        
        if (refValue == null) {
            throw new java.io.IOException("Value not found for key: " + key);
        }
        
        return refValue.openStream();
    }
    
    /**
     * Get the raw internal context map (for legacy compatibility).
     * 
     * @return materialized map of context values
     * @deprecated Use get() or getRef() for individual values
     */
    @Deprecated
    public Map<String, Object> getContext() {
        Map<String, Object> materialized = new HashMap<>();
        for (Map.Entry<String, RefValue> entry : context.entrySet()) {
            materialized.put(entry.getKey(), refValueSupport.refValueToObject(entry.getKey(), entry.getValue()));
        }
        return materialized;
    }

    public boolean containsKey(String key) {
        return context.containsKey(key);
    }

    /**
     * Get a value from the context and mark it as consumed by the specified node.
     * This method also triggers garbage collection for the key if there are no more consumers.
     * If the key is a template reference, it will be resolved before returning.
     * Supports default values using syntax like {{ref:defaultValue}}
     *
     * @param nodeKey The key of the node consuming this value
     * @param key     The key or template to retrieve from the context
     * @return The resolved value from the context, or null if not found
     */
    public Object getAndClean(String nodeKey, String key) {
        String resolvedKey = resolveAlias(key);
        if (isInLoop()) {
            return getAndMarkConsumedInLoop(nodeKey, resolvedKey);
        }
        return getAndMarkConsumed(nodeKey, resolvedKey);
    }


    /**
     * Gets a value from the context and marks it as consumed, handling garbage collection
     *
     * @param nodeKey The consuming node
     * @param key     The key to access
     * @return The value, or null if not found
     */
    private Object getAndMarkConsumed(String nodeKey, String key) {
        if (!context.containsKey(key)) {
            return null;
        }

        RefValue refValue = context.get(key);
        Object value = refValueSupport.refValueToObject(key, refValue);

        // Remove the current node from the consumers list for this key
        removeConsumer(key, nodeKey);

        // Perform garbage collection - remove the key if no more consumers
        performGarbageCollection(key);

        return value;
    }

    /**
     * Gets a value from the context during a loop. Consumer cleanup is deferred
     * until the loop ends to allow multiple iterations to access the same value.
     *
     * @param nodeKey The consuming node
     * @param key     The key to access
     * @return The value, or null if not found
     */
    private Object getAndMarkConsumedInLoop(String nodeKey, String key) {
        if (!context.containsKey(key)) {
            return null;
        }

        RefValue refValue = context.get(key);
        Object value = refValueSupport.refValueToObject(key, refValue);
        
        String activeLoop = getActiveLoop();
        if (activeLoop != null) {
            pendingLoopCleanup
                    .computeIfAbsent(activeLoop, k -> new ConcurrentHashMap<>())
                    .computeIfAbsent(key, k -> new HashSet<>())
                    .add(nodeKey);
        }
        return value;
    }

    /**
     * Resolves a possible aliases to its actual reference key
     *
     * @param key The key that might be an aliases
     * @return The resolved key
     */
    private String resolveAlias(String key) {
        Object resolvedKey = aliases.get(key);
        if (resolvedKey != null) {
            resolvedKey = resolvedKey.toString().substring(2, resolvedKey.toString().length() - 2).trim();
            return resolvedKey.toString();
        } else {
            return key;
        }
    }

    /**
     * Remove a specific consumer from a key's consumer list
     */
    private void removeConsumer(String key, String nodeKey) {
        AtomicInteger remainingConsumers = consumers.get(key);
        if (remainingConsumers == null) {
            return;
        }

        int updated = remainingConsumers.decrementAndGet();
        log.debug("Removed consumer '{}' from key '{}', remaining consumers: {}", nodeKey, key, Math.max(updated, 0));

        if (updated <= 0) {
            consumers.remove(key);
            log.debug("No more consumers for key '{}', removed from consumers map", key);
        }
    }

    /**
     * Perform garbage collection by removing context entries that have no consumers
     */
    private void performGarbageCollection(String key) {
        AtomicInteger remainingConsumers = consumers.get(key);

        if (remainingConsumers == null || remainingConsumers.get() <= 0) {
            RefValue removedValue = context.remove(key);
            if (removedValue != null) {
                log.debug("Garbage collected key '{}' from context", key);
                refValueSupport.releaseRefValue(key, removedValue);
            }
            if (remainingConsumers != null) {
                consumers.remove(key, remainingConsumers);
            } else {
                consumers.remove(key);
            }
        }
    }

    /**
     * Check if a key has any remaining consumers
     */
    public boolean isConsumersEmpty(String key) {
        AtomicInteger keyConsumers = consumers.get(key);
        return keyConsumers == null || keyConsumers.get() <= 0;
    }

    /**
     * Get the remaining consumer count for a specific key
     */
    public int getConsumerCount(String key) {
        AtomicInteger keyConsumers = consumers.get(key);
        return keyConsumers != null ? Math.max(keyConsumers.get(), 0) : 0;
    }

    /**
     * Manual garbage collection for all keys without consumers
     */
    public void garbageCollect() {
        List<String> keysToRemove = new ArrayList<>();

        for (String key : context.keySet()) {
            if (isConsumersEmpty(key)) {
                keysToRemove.add(key);
            }
        }

        for (String key : keysToRemove) {
            context.remove(key);
            consumers.remove(key);
            log.debug("Manual garbage collection removed key '{}'", key);
        }

        if (!keysToRemove.isEmpty()) {
            log.info("Manual garbage collection removed {} unused context entries", keysToRemove.size());
        }
    }

    /**
     * Get current context size for monitoring
     */
    public int getContextSize() {
        return context.size();
    }

    /**
     * Get current consumers map size for monitoring
     */
    public int getConsumersSize() {
        return consumers.size();
    }

    /**
     * Remove a specific key from context and consumers (useful for manual cleanup)
     *
     * @param key The key to remove
     */
    public void remove(String key) {
        RefValue removed = context.remove(key);
        if (removed != null) {
            refValueSupport.releaseRefValue(key, removed);
        }
        consumers.remove(key);
        pendingLoopCleanup.values().forEach(loopMap -> loopMap.remove(key));
        log.debug("Removed key '{}' from context and consumers", key);
    }

    /**
     * Clear all context data (useful for testing or cleanup)
     */
    public void clear() {
        // Release all RefValues before clearing
        for (Map.Entry<String, RefValue> entry : context.entrySet()) {
            refValueSupport.releaseRefValue(entry.getKey(), entry.getValue());
        }
        context.clear();
        consumers.clear();
        pendingLoopCleanup.clear();
        activeLoops.clear();
        log.debug("RuntimeContext cleared");
    }

    // ========== Loop-aware context management methods ==========

    /**
     * Start a loop context. This tells the RuntimeContext to defer cleanup operations
     * for context values accessed within this loop until the loop completes.
     *
     * @param loopNodeKey The unique identifier for the loop node
     */
    public void startLoop(String loopNodeKey) {
        activeLoops.add(loopNodeKey);
        pendingLoopCleanup.put(loopNodeKey, new ConcurrentHashMap<>());
        log.debug("Started loop context for node: {}", loopNodeKey);
    }

    /**
     * End a loop context and perform all deferred cleanup operations.
     * This will clean up all context values that were accessed during the loop
     * and have no remaining consumers.
     *
     * @param loopNodeKey The unique identifier for the loop node
     */
    public void endLoop(String loopNodeKey) {
        if (!activeLoops.contains(loopNodeKey)) {
            log.warn("Attempted to end loop '{}' that was not started", loopNodeKey);
            return;
        }

        activeLoops.remove(loopNodeKey);
        Map<String, Set<String>> pendingCleanup = pendingLoopCleanup.remove(loopNodeKey);

        if (pendingCleanup != null) {
            int cleanedCount = 0;
            for (Map.Entry<String, Set<String>> entry : pendingCleanup.entrySet()) {
                String key = entry.getKey();
                Set<String> nodesConsumed = entry.getValue();
                int consumersToRemove = nodesConsumed != null ? nodesConsumed.size() : 0;

                if (consumersToRemove == 0) {
                    continue;
                }

                AtomicInteger keyConsumers = consumers.get(key);
                if (keyConsumers != null) {
                    int updated = keyConsumers.updateAndGet(current -> Math.max(current - consumersToRemove, 0));
                    if (updated == 0) {
                        consumers.remove(key, keyConsumers);
                    }
                }

                boolean hadValue = context.containsKey(key);
                performGarbageCollection(key);
                if (hadValue && !context.containsKey(key)) {
                    cleanedCount++;
                }
            }

            if (cleanedCount > 0) {
                log.debug("Loop '{}' cleanup removed {} context entries", loopNodeKey, cleanedCount);
            }
        }

        log.debug("Ended loop context for node: {}", loopNodeKey);
    }

    /**
     * Check if there are any active loops currently running
     *
     * @return true if there are active loops
     */
    public boolean isInLoop() {
        return !activeLoops.isEmpty();
    }

    /**
     * Get the active loop for a node key (if any)
     *
     * @return The loop node key if in a loop, null otherwise
     */
    public String getActiveLoop() {
        return activeLoops.stream().findFirst().orElse(null);
    }

    public void endLoopIfActive() {
        String activeLoop = this.getActiveLoop();
        if (activeLoop != null) {
            this.endLoop(activeLoop);
            log.debug("Ended active loop '{}' due to error or validation failure", activeLoop);
        }
    }

    /**
     * Write a value to the pending writes buffer with explicit options.
     * The write will be staged until flushPendingWrites() is called.
     * 
     * @param key context key
     * @param value value to write
     * @param options storage options (mediaType, storage preference, auto-cleanup)
     */
    public void write(String key, Object value, WriteOptions options) {
        pendingWrites.put(key, new PendingWrite(value, options));
    }

    /**
     * Write a value to the pending writes buffer with default options.
     * 
     * @param key context key
     * @param value value to write
     */
    public void write(String key, Object value) {
        write(key, value, WriteOptions.DEFAULT);
    }
    
    /**
     * Write a value from an InputStream to the pending writes buffer.
     * The stream will be consumed progressively and written directly to storage
     * without loading the entire content into memory.
     * The stream will be closed by this method.
     * 
     * <p>This is efficient for large binary data as it streams directly to disk
     * without intermediate buffering in memory.
     * 
     * <p><b>Note:</b> Unlike {@link #write(String, Object, WriteOptions)}, this method
     * immediately creates the RefValue and stores it directly, bypassing the pending
     * writes buffer. This is necessary because InputStreams cannot be buffered
     * (they can only be read once).
     * 
     * @param key context key (relative, will be scoped during flush)
     * @param inputStream stream to read data from (will be closed)
     * @param options storage options (mediaType, storage preference, auto-cleanup)
     * @throws IOException if the stream cannot be read
     */
    public void writeStream(String key, InputStream inputStream, WriteOptions options) throws IOException {
        // Create RefValue directly from stream for progressive write
        // We can't buffer streams like we do with regular writes
        RefValue refValue = refValueSupport.createRefValueFromStream(inputStream, options.storage(), options.mediaType());
        
        // Store in pending buffer as RefValue (not as InputStream)
        // When flushed, this RefValue will be moved to the scoped key
        pendingWrites.put(key, new PendingWrite(refValue, options));
    }
    
    /**
     * Write a value from an InputStream with default options.
     * 
     * @param key context key
     * @param inputStream stream to read data from (will be closed)
     * @throws IOException if the stream cannot be read
     */
    public void writeStream(String key, InputStream inputStream) throws IOException {
        writeStream(key, inputStream, WriteOptions.DEFAULT);
    }

    /**
     * Get a copy of pending writes for DB history logging.
     * Returns a snapshot of values before they are flushed to context storage.
     * 
     * @return immutable map of pending writes (key → value)
     */
    public Map<String, Object> getPendingWrites() {
        if (pendingWrites.isEmpty()) {
            return Map.of();
        }

        Map<String, Object> snapshot = new HashMap<>();
        for (Map.Entry<String, PendingWrite> entry : pendingWrites.entrySet()) {
            snapshot.put(entry.getKey(), entry.getValue().value());
        }
        return Collections.unmodifiableMap(snapshot);
    }

    /**
     * Flush pending writes to RefValue storage.
     * Called by WorkflowEngineService after successful execution.
     * Stores pending write directly with their WriteOptions preserved.
     * @param nodeKey All keys are normalizing through this nodeKey
     */
    public void flushPendingWrites(String nodeKey) {
        if (pendingWrites.isEmpty()) {
            return;
        }
        
        // Convert pending writes to Map and delegate to processOutputWithMetadata
        // This ensures consistent selective storage and automatic RefValue creation
        for (Map.Entry<String, PendingWrite> entry : pendingWrites.entrySet()) {
            String scopeKey = ContextKeyResolver.scopeKey(nodeKey, entry.getKey());
            PendingWrite pending = entry.getValue();

            // Check if this key has consumers (selective storage)
            if (isConsumersEmpty(scopeKey)) {
                log.debug("Skipping storage of '{}' as it has no registered consumers", scopeKey);
                continue;
            }

            // Check if the value is already a RefValue (from writeStream)
            RefValue refValue;
            if (pending.value() instanceof RefValue existingRef) {
                // Already a RefValue from writeStream(), use it directly
                refValue = existingRef;
                log.debug("Using pre-created RefValue for '{}' (from writeStream)", scopeKey);
            } else {
                // Create RefValue with explicit WriteOptions
                refValue = refValueSupport.createRefValue(
                        pending.value(),
                        pending.options().storage(),
                        pending.options().mediaType()
                );
            }
            
            context.put(scopeKey, refValue);
            log.debug("Stored pending write '{}' with options: {}", scopeKey, pending.options());
        }
        
        pendingWrites.clear();
    }

    /**
     * Discard pending writes without persisting.
     * Called by WorkflowEngineService on execution error.
     */
    public void clearPendingWrites() {
        pendingWrites.clear();
    }
}
