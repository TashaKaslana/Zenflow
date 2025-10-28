package org.phong.zenflow.workflow.subdomain.context.refvalue;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.workflow.subdomain.context.refvalue.common.ObjectStructureHelper;
import org.phong.zenflow.workflow.subdomain.context.refvalue.dto.StoragePreference;
import org.phong.zenflow.workflow.subdomain.context.refvalue.impl.FileRefValue;
import org.phong.zenflow.workflow.subdomain.context.refvalue.impl.JsonRefValue;
import org.phong.zenflow.workflow.subdomain.context.refvalue.impl.MemoryRefValue;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Base64;

/**
 * Factory for creating RefValue instances with intelligent storage backend selection.
 * 
 * <p>Selection heuristics (optimized for million-request scale):
 * <ul>
 *   <li>Small objects (< 1MB) → MemoryRefValue</li>
 *   <li>JSON structures (1-2MB) → JsonRefValue</li>
 *   <li>Large payloads (> 1MB) → FileRefValue</li>
 *   <li>Base64 strings (decoded > 512KB) → FileRefValue with decoded bytes</li>
 *   <li>Binary streams → FileRefValue</li>
 * </ul>
 * 
 * <p>Safety overrides ensure thresholds are respected even when explicit preferences are given.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RefValueFactory {
    private final static int fallbackSize = 1024;
    private final RefValueConfig config;
    
    /**
     * Creates a RefValue from an object with auto storage selection.
     * 
     * @param value the value to wrap
     * @return appropriate RefValue implementation
     */
    public RefValue create(Object value) {
        return create(value, StoragePreference.AUTO, null);
    }
    
    /**
     * Creates a RefValue with a storage preference hint.
     * 
     * @param value the value to wrap
     * @param preference storage hint (may be overridden by safety checks)
     * @return appropriate RefValue implementation
     */
    public RefValue create(Object value, StoragePreference preference) {
        return create(value, preference, null);
    }
    
    /**
     * Creates a RefValue with storage preference and media type hint.
     * 
     * @param value the value to wrap
     * @param preference storage hint
     * @param mediaType content type (e.g., "application/json", "video/mp4")
     * @return appropriate RefValue implementation
     */
    public RefValue create(Object value, StoragePreference preference, String mediaType) {
        if (value == null) {
            return new MemoryRefValue(null, mediaType);
        }
        
        try {
            // Only decode base64 if EXPLICITLY marked with mediaType
            if (mediaType != null && mediaType.equals("text/base64") && value instanceof String str) {
                return handleBase64String(str, preference, mediaType);
            }
            
            // Estimate size for threshold decisions
            long estimatedSize = ObjectStructureHelper.estimateSize(value, fallbackSize);
            
            // Apply AUTO heuristics
            if (preference == StoragePreference.AUTO) {
                preference = selectStorageType(value, estimatedSize, mediaType);
            }
            
            // Safety override: large values must go to file regardless of preference
            if (estimatedSize > config.getMemoryThresholdBytes() && preference == StoragePreference.MEMORY) {
                log.warn("Overriding MEMORY preference for large value ({}  bytes) to FILE", estimatedSize);
                preference = StoragePreference.FILE;
            }
            
            // Create based on final preference
            return switch (preference) {
                case JSON -> createJsonRefValue(value, estimatedSize);
                case FILE -> createFileRefValue(value, mediaType, estimatedSize);
                default -> new MemoryRefValue(value, mediaType);
            };
            
        } catch (Exception e) {
            log.error("Failed to create RefValue, falling back to memory storage", e);
            return new MemoryRefValue(value, mediaType);
        }
    }
    
    /**
     * Creates a RefValue directly from an InputStream for progressive streaming writes.
     * The stream will be consumed and written directly to disk without loading the
     * entire content into memory. This is the most efficient way to handle large
     * binary payloads.
     * 
     * <p>The stream will be closed by this method (via try-with-resources).
     * 
     * <p><b>Storage preference handling:</b>
     * <ul>
     *   <li>FILE - writes directly to file (streaming)</li>
     *   <li>AUTO - uses FILE (streaming is typically for large data)</li>
     *   <li>MEMORY - reads entire stream into byte[] then stores in memory</li>
     *   <li>JSON - not supported, falls back to FILE</li>
     * </ul>
     * 
     * @param inputStream stream to read from (will be closed)
     * @param preference storage preference (FILE or AUTO recommended)
     * @param mediaType content type hint (e.g., "video/mp4", "application/octet-stream")
     * @return RefValue backed by streamed data
     * @throws IOException if stream cannot be read or written
     */
    public RefValue createFromStream(java.io.InputStream inputStream, 
                                     StoragePreference preference, 
                                     String mediaType) throws IOException {
        // AUTO preference defaults to FILE for streams (streaming typically means large data)
        if (preference == StoragePreference.AUTO) {
            preference = StoragePreference.FILE;
        }
        
        // JSON storage doesn't make sense for raw streams, use FILE instead
        if (preference == StoragePreference.JSON) {
            log.debug("JSON storage not supported for streams, using FILE instead");
            preference = StoragePreference.FILE;
        }

        // Try-with-resources ensures stream is closed even on error
        try (inputStream) {
            if (preference == StoragePreference.MEMORY) {
                // For MEMORY, we have to read the entire stream (defeats streaming purpose)
                log.warn("MEMORY storage for streams requires loading entire content - consider using FILE");
                byte[] data = inputStream.readAllBytes();
                return new MemoryRefValue(data, mediaType);
            }

            // FILE storage: progressive streaming write
            Path targetDir = config.getFileStoragePath();
            String prefix = config.getTempFilePrefix();
            return FileRefValue.fromStream(inputStream, mediaType, targetDir, prefix);
        }
    }

    /**
     * Reconstructs a RefValue from a persisted descriptor.
     * 
     * <p><b>WARNING:</b> This feature is incomplete. File-backed values may not
     * survive workflow restarts if files are cleaned up. Use with caution.
     * 
     * @param descriptor persisted descriptor
     * @return reconstructed RefValue
     * @throws IOException if reconstruction fails
     */
    public RefValue fromDescriptor(RefValueDescriptor descriptor) throws IOException {
        if (descriptor == null) {
            return new MemoryRefValue(null);
        }
        
        return switch (descriptor.getType()) {
            case MEMORY -> new MemoryRefValue(descriptor.getInlineValue(), descriptor.getMediaType());
            case JSON -> {
                if (descriptor.getInlineValue() instanceof byte[] bytes) {
                    yield JsonRefValue.fromBytes(bytes);
                }
                throw new IOException("Invalid JSON descriptor: missing inline value");
            }
            case FILE -> {
                if (descriptor.getLocator() == null) {
                    throw new IOException("Invalid FILE descriptor: missing locator");
                }
                Path path = Path.of(descriptor.getLocator());
                // WARNING: File may have been deleted since workflow pause/restart
                yield new FileRefValue(path, descriptor.getMediaType());
            }
        };
    }
    
    /**
     * Selects storage type based on value characteristics and thresholds.
     */
    private StoragePreference selectStorageType(Object value, long estimatedSize, String mediaType) {
        // Check JSON structures first (before size-only checks)
        // JSON between 1-2MB should use JsonRefValue for optimized queries
        if (ObjectStructureHelper.isJsonStructure(value, mediaType)) {
            if (estimatedSize > config.getMemoryThresholdBytes() && 
                estimatedSize <= config.getJsonThresholdBytes()) {
                // 1MB < size <= 2MB: Use JsonRefValue for fast JsonPointer access
                return StoragePreference.JSON;
            }
            // Very large JSON (> 2MB) goes to file
            if (estimatedSize > config.getJsonThresholdBytes()) {
                return StoragePreference.FILE;
            }
            // Small JSON (< 1MB) uses memory
            return StoragePreference.MEMORY;
        }
        
        // Binary data goes to file if large
        if (value instanceof byte[] && estimatedSize > config.getMemoryThresholdBytes()) {
            return StoragePreference.FILE;
        }
        
        // Large non-JSON values go to file
        if (estimatedSize > config.getMemoryThresholdBytes()) {
            return StoragePreference.FILE;
        }
        
        // Default: memory storage for small values
        return StoragePreference.MEMORY;
    }
    
    /**
     * Handles base64 string detection and decoding.
     */
    private RefValue handleBase64String(String str, StoragePreference preference, String mediaType) throws IOException {
        try {
            byte[] decoded = Base64.getDecoder().decode(str);
            long decodedSize = decoded.length;
            
            log.debug("Detected base64 string: {} encoded → {} decoded bytes", 
                    str.length(), decodedSize);
            
            // If decoded size exceeds threshold, store as file
            if (decodedSize > config.getBase64ThresholdBytes()) {
                Path targetDir = config.getFileStoragePath();
                String prefix = config.getTempFilePrefix();
                return FileRefValue.fromBytes(decoded, mediaType, targetDir, prefix);
            }
            
            // Small decoded data stays in memory
            return new MemoryRefValue(decoded, mediaType);
            
        } catch (IllegalArgumentException e) {
            // Not valid base64 after all, treat as regular string
            log.trace("String looked like base64 but failed to decode, treating as plain text");
            return new MemoryRefValue(str, mediaType);
        }
    }
    
    /**
     * Creates a JsonRefValue, falling back to FileRefValue if too large.
     */
    private RefValue createJsonRefValue(Object value, long estimatedSize) throws IOException {
        if (estimatedSize > config.getJsonThresholdBytes()) {
            log.debug("JSON value too large ({} bytes), using FileRefValue instead", estimatedSize);
            return createFileRefValue(value, "application/json", estimatedSize);
        }
        
        if (value instanceof JsonNode node) {
            return new JsonRefValue(node);
        }
        
        if (value instanceof String str) {
            return JsonRefValue.fromString(str);
        }
        
        // Convert object to JSON tree
        return JsonRefValue.fromObject(value);
    }
    
    /**
     * Creates a FileRefValue from various value types.
     */
    private RefValue createFileRefValue(Object value, String mediaType, long estimatedSize) throws IOException {
        Path targetDir = config.getFileStoragePath();
        String prefix = config.getTempFilePrefix();
        
        if (value instanceof byte[] bytes) {
            return FileRefValue.fromBytes(bytes, mediaType, targetDir, prefix);
        }
        
        if (value instanceof String str) {
            // Store strings as UTF-8 text, not JSON, to preserve original type
            byte[] bytes = str.getBytes(java.nio.charset.StandardCharsets.UTF_8);
            return FileRefValue.fromBytes(bytes, "text/plain; charset=UTF-8", targetDir, prefix);
        }
        
        // Store all other values as JSON
        return FileRefValue.fromObject(value, targetDir, prefix);
    }
}
