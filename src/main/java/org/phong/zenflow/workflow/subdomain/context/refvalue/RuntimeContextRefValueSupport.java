package org.phong.zenflow.workflow.subdomain.context.refvalue;

import lombok.extern.slf4j.Slf4j;

import org.phong.zenflow.workflow.subdomain.context.refvalue.dto.StoragePreference;
import org.springframework.stereotype.Component;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.HashMap;

/**
 * Support layer for RefValue operations in RuntimeContext.
 * Handles conversion between Object and RefValue, cleanup integration,
 * and metrics tracking.
 * 
 * <p>This class is designed as a helper to keep RuntimeContext clean
 * during the migration to RefValue-based storage.
 */
@Slf4j
@Component
public class RuntimeContextRefValueSupport {
    
    private final RefValueFactory factory;
    private final RefValueMetrics metrics;
    
    /**
     * Constructor for Spring injection.
     */
    public RuntimeContextRefValueSupport(RefValueFactory factory, RefValueMetrics metrics) {
        this.factory = factory;
        this.metrics = metrics;
    }
    
    /**
     * Default constructor for non-Spring usage (creates minimal dependencies).
     */
    public RuntimeContextRefValueSupport() {
        RefValueConfig config = new RefValueConfig();
        this.metrics = new RefValueMetrics(new SimpleMeterRegistry(), config);
        this.factory = new RefValueFactory(config);
    }
    
    /**
     * Converts an Object to a RefValue using auto-selection.
     * 
     * @param key context key (for logging/metrics)
     * @param value object to wrap
     * @return RefValue instance
     */
    public RefValue objectToRefValue(String key, Object value) {
        try {
            RefValue ref = factory.create(value, StoragePreference.AUTO, null);
            log.trace("Created RefValue for key '{}': type={}, size={}", 
                    key, ref.getType(), ref.getSize());
            return ref;
        } catch (Exception e) {
            log.error("Failed to create RefValue for key '{}', using fallback", key, e);
            // Fallback: wrap in MemoryRefValue via factory with explicit preference
            return factory.create(value, StoragePreference.MEMORY, null);
        }
    }
    
    /**
     * Converts multiple entries to RefValues.
     * 
     * @param entries map of entries to convert
     * @return map of RefValues
     */
    public Map<String, RefValue> objectMapToRefValueMap(Map<String, Object> entries) {
        return entries.entrySet().stream()
                .collect(
                        HashMap::new,
                        (map, entry) -> map.put(entry.getKey(), objectToRefValue(entry.getKey(), entry.getValue())),
                        HashMap::putAll
                );
    }
    
    /**
     * Materializes a RefValue to an Object (for backward compatibility).
     * 
     * @param key context key (for logging)
     * @param refValue the RefValue to materialize
     * @return plain object, or null
     */
    public Object refValueToObject(String key, RefValue refValue) {
        if (refValue == null) {
            return null;
        }
        
        try {
            Object result = refValue.read(Object.class);
            log.trace("Materialized RefValue for key '{}': type={}", key, refValue.getType());
            return result;
        } catch (IOException e) {
            log.error("Failed to materialize RefValue for key '{}'", key, e);
            return null;
        }
    }
    
    /**
     * Safely releases a RefValue, catching any exceptions.
     * 
     * @param key context key (for logging)
     * @param refValue the RefValue to release
     */
    public void releaseRefValue(String key, RefValue refValue) {
        if (refValue == null) {
            return;
        }
        
        try {
            if (metrics != null) {
                metrics.recordCleanupDuration(refValue::onRelease);
            } else {
                refValue.onRelease();
            }
            log.trace("Released RefValue for key '{}': type={}, size={}", 
                    key, refValue.getType(), refValue.getSize());
        } catch (Exception e) {
            log.error("Failed to release RefValue for key '{}'", key, e);
        }
    }

    /**
     * Creates a RefValue with explicit options.
     * Used by the pending writes mechanism to control storage behavior.
     * 
     * @param value the value to wrap
     * @param storage storage preference (AUTO, MEMORY, JSON, FILE)
     * @param mediaType content type hint (e.g., "text/base64", "application/json")
     * @return RefValue instance
     */
    public RefValue createRefValue(Object value, StoragePreference storage, String mediaType) {
        try {
            RefValue ref = factory.create(value, storage, mediaType);
            log.trace("Created RefValue with explicit options: type={}, size={}, mediaType={}", 
                    ref.getType(), ref.getSize(), mediaType);
            return ref;
        } catch (Exception e) {
            log.error("Failed to create RefValue with explicit options, using fallback", e);
            // Fallback: use auto preference without mediaType
            return factory.create(value, StoragePreference.AUTO, null);
        }
    }
    
    /**
     * Creates a RefValue directly from an InputStream for progressive/streaming writes.
     * The stream will be consumed and written directly to storage without loading
     * the entire content into memory.
     * 
     * <p>This is efficient for large binary payloads (videos, large files, etc.) as it
     * uses {@link Files#copy(InputStream, Path, CopyOption...)}
     * which streams data directly to disk.
     * 
     * @param inputStream stream to read data from (will be closed)
     * @param storage storage preference (AUTO will typically use FILE for streams)
     * @param mediaType content type hint (e.g., "video/mp4", "application/octet-stream")
     * @return RefValue instance backed by streamed data
     * @throws IOException if the stream cannot be read or written
     */
    public RefValue createRefValueFromStream(InputStream inputStream,
                                             StoragePreference storage,
                                             String mediaType) throws IOException {
        return factory.createFromStream(inputStream, storage, mediaType);
    }
}

