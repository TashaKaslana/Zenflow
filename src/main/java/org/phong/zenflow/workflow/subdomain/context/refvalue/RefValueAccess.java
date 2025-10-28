package org.phong.zenflow.workflow.subdomain.context.refvalue;

import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;
import java.io.InputStream;

/**
 * Helper interface passed to ReadFunction implementations, providing
 * backend-optimized methods for extracting data from a RefValue.
 * 
 * <p>Different RefValue implementations provide different capabilities:
 * <ul>
 *   <li>MemoryRefValue: direct object access</li>
 *   <li>JsonRefValue: streaming JSON parsing, JsonPointer navigation</li>
 *   <li>FileRefValue: byte streaming, optional deserialization</li>
 * </ul>
 */
public interface RefValueAccess {
    
    /**
     * Materializes the value as an instance of the target type.
     * 
     * @param <T> target type
     * @param targetType class to cast/convert to
     * @return materialized object
     * @throws IOException if deserialization fails
     */
    <T> T asObject(Class<T> targetType) throws IOException;
    
    /**
     * Returns a streaming parser over the JSON value (if applicable).
     * Throws UnsupportedOperationException if the value is not JSON-structured.
     * 
     * @return JsonNode tree for navigation
     * @throws IOException if parsing fails
     * @throws UnsupportedOperationException if not a JSON value
     */
    JsonNode asJsonTree() throws IOException;
    
    /**
     * Extracts a JSON subtree at the given JsonPointer path.
     * Example: "/user/address/city" extracts just the city field.
     * 
     * @param pointer JSON Pointer expression (RFC 6901)
     * @return subtree at the pointer location, or null if path doesn't exist
     * @throws IOException if parsing fails
     * @throws UnsupportedOperationException if not a JSON value
     */
    JsonNode jsonAt(String pointer) throws IOException;
    
    /**
     * Opens a stream over the raw bytes.
     * 
     * @return byte stream
     * @throws IOException if stream cannot be opened
     */
    InputStream streamBytes() throws IOException;
    
    /**
     * Returns metadata about the value.
     * 
     * @return size in bytes, or -1 if unknown
     */
    long getSize();
}
