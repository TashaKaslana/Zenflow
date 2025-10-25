package org.phong.zenflow.workflow.subdomain.context.refvalue;

/**
 * Enumeration of storage backend types for RefValue implementations.
 * Each type represents a different strategy for storing and retrieving context values.
 */
public enum RefValueType {
    /**
     * Value stored directly in heap memory (default for small objects)
     */
    MEMORY,
    
    /**
     * Value stored as parsed JSON tree with optimized access patterns
     */
    JSON,
    
    /**
     * Value stored in temporary file on disk
     */
    FILE
}
