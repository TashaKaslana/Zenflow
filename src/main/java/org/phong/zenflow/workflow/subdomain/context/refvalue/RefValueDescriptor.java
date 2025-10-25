package org.phong.zenflow.workflow.subdomain.context.refvalue;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

import java.io.Serializable;

/**
 * Serializable descriptor for a RefValue, containing metadata needed to
 * reconstruct the value from persistent storage (database, files).
 * 
 * <p>Stored in WorkflowRun.context as JSON, replacing the previous Map<String,Object>.
 * 
 * <p><b>WARNING:</b> Persistence/restoration of RefValues is currently incomplete.
 * File-backed values may not survive workflow restarts. This feature is under development.
 * 
 * @see RefValue#toDescriptor()
 * @see RefValueFactory#fromDescriptor(RefValueDescriptor)
 */
@Value
@Builder
public class RefValueDescriptor implements Serializable {
    
    /**
     * Backend type (MEMORY, JSON, FILE)
     */
    RefValueType type;
    
    /**
     * Location identifier (file path, memory reference, etc.)
     * May be null for memory-only values.
     */
    String locator;
    
    /**
     * Media/content type (e.g., "application/json", "video/mp4")
     */
    String mediaType;
    
    /**
     * Estimated size in bytes, or -1 if unknown
     */
    long size;
    
    /**
     * Optional checksum for integrity verification
     */
    String checksum;
    
    /**
     * For MEMORY type, stores the serialized value inline.
     * For other types, this field is typically null.
     */
    Object inlineValue;
    
    @JsonCreator
    public RefValueDescriptor(
            @JsonProperty("type") RefValueType type,
            @JsonProperty("locator") String locator,
            @JsonProperty("mediaType") String mediaType,
            @JsonProperty("size") long size,
            @JsonProperty("checksum") String checksum,
            @JsonProperty("inlineValue") Object inlineValue) {
        this.type = type;
        this.locator = locator;
        this.mediaType = mediaType;
        this.size = size;
        this.checksum = checksum;
        this.inlineValue = inlineValue;
    }
}
