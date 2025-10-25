package org.phong.zenflow.workflow.subdomain.context.refvalue.impl;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.core.utils.ObjectConversion;
import org.phong.zenflow.workflow.subdomain.context.refvalue.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

/**
 * Simple in-memory storage for small objects (default behavior).
 * Wraps the raw object with no externalization or streaming support.
 * 
 * <p>Used for:
 * <ul>
 *   <li>Small strings, numbers, booleans</li>
 *   <li>Small maps/lists (< threshold)</li>
 *   <li>Any object when explicit MEMORY preference is given</li>
 * </ul>
 * 
 * <p>No cleanup needed as object lives in heap and will be GC'd normally.
 */
@Slf4j
public class MemoryRefValue implements RefValue {
    
    private final Object value;
    private final String mediaType;
    private final long estimatedSize;
    private final RefValueMetrics metrics;
    
    public MemoryRefValue(Object value) {
        this(value, null, null);
    }
    
    public MemoryRefValue(Object value, String mediaType) {
        this(value, mediaType, null);
    }
    
    public MemoryRefValue(Object value, String mediaType, RefValueMetrics metrics) {
        this.value = value;
        this.mediaType = mediaType;
        this.estimatedSize = estimateSize(value);
        this.metrics = metrics;
        
        if (metrics != null) {
            metrics.recordCreated(RefValueType.MEMORY, estimatedSize, null);
        }
    }
    
    @Override
    public RefValueType getType() {
        return RefValueType.MEMORY;
    }
    
    @Override
    public long getSize() {
        return estimatedSize;
    }
    
    @Override
    public Optional<String> getMediaType() {
        return Optional.ofNullable(mediaType);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> T read(Class<T> targetType) throws IOException {
        if (value == null) {
            return null;
        }
        if (targetType.isInstance(value)) {
            return (T) value;
        }
        // Attempt conversion for common cases
        if (targetType == String.class) {
            return (T) String.valueOf(value);
        }
        throw new ClassCastException("Cannot cast " + value.getClass() + " to " + targetType);
    }
    
    @Override
    public <R> R read(ReadFunction<R> reader) throws IOException {
        return reader.apply(new MemoryRefValueAccess());
    }
    
    @Override
    public InputStream openStream() throws IOException {
        if (value == null) {
            return new ByteArrayInputStream(new byte[0]);
        }
        // Serialize to JSON bytes for generic streaming
        byte[] bytes = ObjectConversion.getObjectMapper().writeValueAsBytes(value);
        return new ByteArrayInputStream(bytes);
    }
    
    @Override
    public RefValueDescriptor toDescriptor() {
        return RefValueDescriptor.builder()
                .type(RefValueType.MEMORY)
                .mediaType(mediaType)
                .size(estimatedSize)
                .inlineValue(value)
                .build();
    }
    
    @Override
    public void onRelease() {
        // No-op: memory values are garbage collected naturally
        if (metrics != null) {
            metrics.recordReleased(RefValueType.MEMORY, estimatedSize, null);
        }
        log.trace("Released memory RefValue (size: {} bytes)", estimatedSize);
    }
    
    /**
     * Rough size estimation for monitoring purposes.
     * Not precise, but good enough for threshold decisions.
     */
    private long estimateSize(Object obj) {
        if (obj == null) return 0;
        if (obj instanceof String s) return s.length() * 2L; // Rough UTF-16 estimate
        if (obj instanceof byte[] b) return b.length;
        if (obj instanceof Number) return 8;
        if (obj instanceof Boolean) return 1;
        // For complex objects, serialize to estimate
        try {
            return ObjectConversion.getObjectMapper().writeValueAsBytes(obj).length;
        } catch (Exception e) {
            log.debug("Could not estimate size for {}", obj.getClass(), e);
            return -1;
        }
    }
    
    /**
     * Access helper for memory values - provides direct object access.
     */
    private class MemoryRefValueAccess implements RefValueAccess {
        
        @Override
        public <T> T asObject(Class<T> targetType) throws IOException {
            return read(targetType);
        }
        
        @Override
        public JsonNode asJsonTree() throws IOException {
            if (value == null) {
                return ObjectConversion.getObjectMapper().nullNode();
            }
            return ObjectConversion.getObjectMapper().valueToTree(value);
        }
        
        @Override
        public JsonNode jsonAt(String pointer) throws IOException {
            JsonNode tree = asJsonTree();
            return tree.at(pointer);
        }
        
        @Override
        public InputStream streamBytes() throws IOException {
            return openStream();
        }
        
        @Override
        public long getSize() {
            return estimatedSize;
        }
    }
}
