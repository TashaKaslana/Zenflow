package org.phong.zenflow.workflow.subdomain.context.refvalue.impl;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.core.utils.ObjectConversion;
import org.phong.zenflow.workflow.subdomain.context.refvalue.*;
import org.phong.zenflow.workflow.subdomain.context.refvalue.dto.RefValueType;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Optional;

/**
 * Optimized storage for JSON-structured data with streaming access.
 * Stores parsed JsonNode tree for efficient JsonPointer queries without
 * full materialization of nested structures.
 * 
 * <p>For very large JSON (> threshold), consider using FileRefValue with JSON media type.
 * This implementation keeps the tree in memory for fast access.
 * 
 * <p>Use cases:
 * <ul>
 *   <li>Medium-sized JSON documents (100KB - 3MB)</li>
 *   <li>Frequent JsonPointer queries on nested data</li>
 *   <li>API responses that need selective field extraction</li>
 * </ul>
 */
@Slf4j
public class JsonRefValue implements RefValue {
    
    private final JsonNode tree;
    private final byte[] serializedBytes; // Cached for streaming
    private final long size;
    
    /**
     * Creates a JsonRefValue from a parsed JsonNode.
     * 
     * @param tree the JSON tree to wrap
     */
    public JsonRefValue(JsonNode tree) {
        this.tree = tree;
        try {
            this.serializedBytes = ObjectConversion.getObjectMapper().writeValueAsBytes(tree);
            this.size = serializedBytes.length;
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to serialize JSON tree", e);
        }
    }
    
    /**
     * Creates a JsonRefValue by parsing JSON from a string.
     * 
     * @param jsonString raw JSON string
     * @return parsed JsonRefValue
     * @throws IOException if parsing fails
     */
    public static JsonRefValue fromString(String jsonString) throws IOException {
        JsonNode tree = ObjectConversion.getObjectMapper().readTree(jsonString);
        return new JsonRefValue(tree);
    }
    
    /**
     * Creates a JsonRefValue by parsing JSON from bytes.
     * 
     * @param jsonBytes raw JSON bytes
     * @return parsed JsonRefValue
     * @throws IOException if parsing fails
     */
    public static JsonRefValue fromBytes(byte[] jsonBytes) throws IOException {
        JsonNode tree = ObjectConversion.getObjectMapper().readTree(jsonBytes);
        return new JsonRefValue(tree);
    }
    
    /**
     * Creates a JsonRefValue from any object by converting to JSON tree.
     * 
     * @param obj object to convert
     * @return JsonRefValue wrapping the object's JSON representation
     */
    public static JsonRefValue fromObject(Object obj) {
        JsonNode tree = ObjectConversion.getObjectMapper().valueToTree(obj);
        return new JsonRefValue(tree);
    }
    
    @Override
    public RefValueType getType() {
        return RefValueType.JSON;
    }
    
    @Override
    public long getSize() {
        return size;
    }
    
    @Override
    public Optional<String> getMediaType() {
        return Optional.of("application/json");
    }
    
    @Override
    public <T> T read(Class<T> targetType) throws IOException {
        if (tree == null || tree.isNull()) {
            return null;
        }
        return ObjectConversion.getObjectMapper().treeToValue(tree, targetType);
    }
    
    @Override
    public <R> R read(ReadFunction<R> reader) throws IOException {
        return reader.apply(new JsonRefValueAccess());
    }
    
    @Override
    public InputStream openStream() {
        return new ByteArrayInputStream(serializedBytes);
    }
    
    @Override
    public RefValueDescriptor toDescriptor() {
        // For persistence, we store the JSON as inline value
        // For very large JSON, consider switching to FILE type instead
        return RefValueDescriptor.builder()
                .type(RefValueType.JSON)
                .mediaType("application/json")
                .size(size)
                .inlineValue(serializedBytes)
                .build();
    }
    
    @Override
    public void onRelease() {
        // No-op: JSON tree is garbage collected naturally
        log.trace("Released JSON RefValue (size: {} bytes)", size);
    }
    
    /**
     * Access helper providing optimized JSON operations.
     */
    private class JsonRefValueAccess implements RefValueAccess {
        
        @Override
        public <T> T asObject(Class<T> targetType) throws IOException {
            return read(targetType);
        }
        
        @Override
        public JsonNode asJsonTree() {
            return tree;
        }
        
        @Override
        public JsonNode jsonAt(String pointer) {
            if (pointer == null || pointer.isEmpty()) {
                return tree;
            }
            return tree.at(pointer);
        }
        
        @Override
        public InputStream streamBytes() {
            return openStream();
        }
        
        @Override
        public long getSize() {
            return size;
        }
    }
    
    /**
     * Convenience method to extract a value at a JsonPointer path.
     * 
     * @param pointer JSON Pointer expression (e.g., "/user/name")
     * @param targetType class to convert to
     * @param <T> target type
     * @return value at the pointer location, or null
     * @throws IOException if conversion fails
     */
    public <T> T getAt(String pointer, Class<T> targetType) throws IOException {
        JsonNode node = tree.at(pointer);
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        return ObjectConversion.getObjectMapper().treeToValue(node, targetType);
    }
}
