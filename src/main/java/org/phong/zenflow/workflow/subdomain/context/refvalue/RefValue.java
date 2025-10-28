package org.phong.zenflow.workflow.subdomain.context.refvalue;

import org.phong.zenflow.workflow.subdomain.context.refvalue.dto.RefValueType;

import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

/**
 * Abstraction for context values that can be stored in different backends
 * (memory, file, parsed JSON trees) without forcing full materialization.
 * 
 * <p>RefValue instances are managed by RuntimeContext and follow a lifecycle:
 * <ul>
 *   <li>Creation via RefValueFactory based on payload size/type</li>
 *   <li>Access through read() methods (full or selective)</li>
 *   <li>Cleanup via onRelease() when consumer count reaches zero</li>
 * </ul>
 * 
 * <p>RuntimeContext maintains consumer reference counts; RefValue implementations
 * only handle resource disposal when instructed via onRelease().
 * 
 * @see RefValueFactory
 * @see org.phong.zenflow.workflow.subdomain.context.RuntimeContext
 */
public interface RefValue extends AutoCloseable {
    
    /**
     * Returns the storage backend type for this value.
     * 
     * @return the backend type (MEMORY, JSON, FILE)
     */
    RefValueType getType();
    
    /**
     * Returns the estimated size of the stored value in bytes.
     * 
     * @return size in bytes, or -1 if size cannot be determined
     */
    long getSize();
    
    /**
     * Returns the media/content type if known (e.g., "application/json", "video/mp4").
     * 
     * @return optional media type string
     */
    Optional<String> getMediaType();
    
    /**
     * Materializes the full value as an instance of the target type.
     * This method provides backward compatibility with existing code expecting
     * plain objects from RuntimeContext.get().
     * 
     * <p>For large values, this may trigger full materialization into heap memory.
     * Consider using read(ReadFunction) for selective extraction when possible.
     * 
     * @param <T> target type
     * @param targetType class to cast/convert to
     * @return materialized value, or null if value is null
     * @throws IOException if reading/deserialization fails
     */
    <T> T read(Class<T> targetType) throws IOException;
    
    /**
     * Provides selective access to the value without full materialization.
     * The ReadFunction receives a RefValueAccess helper that exposes streaming,
     * tree navigation, or direct access depending on the backend.
     * 
     * <p>Example: extracting a JSON subtree without parsing the entire document.
     * 
     * @param <R> result type
     * @param reader function to extract the desired projection
     * @return the extracted result
     * @throws IOException if reading fails
     */
    <R> R read(ReadFunction<R> reader) throws IOException;
    
    /**
     * Opens a stream to the raw value bytes.
     * Useful for large binary payloads or when streaming to external systems.
     * 
     * <p>Caller is responsible for closing the returned stream.
     * 
     * @return input stream over the value
     * @throws IOException if stream cannot be opened
     */
    InputStream openStream() throws IOException;
    
    /**
     * Converts this value into a descriptor for persistence.
     * The descriptor contains metadata (type, locator, size) that can be
     * serialized to the database and later reconstructed into a RefValue.
     * 
     * @return serializable descriptor
     */
    RefValueDescriptor toDescriptor();
    
    /**
     * Callback invoked by RuntimeContext when this value is acquired by a consumer.
     * Default implementation does nothing; override if resource preparation is needed.
     */
    default void onAcquire() {
        // Default: no-op
    }
    
    /**
     * Callback invoked by RuntimeContext when consumer count reaches zero.
     * Implementations should release backend resources (delete temp files, etc.).
     * 
     * <p>This method must be idempotent as it may be called multiple times.
     */
    void onRelease();
    
    /**
     * AutoCloseable implementation delegates to onRelease().
     */
    @Override
    default void close() {
        onRelease();
    }
}
