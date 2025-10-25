package org.phong.zenflow.workflow.subdomain.context.refvalue;

/**
 * Hint for storage preference when creating a RefValue.
 * The factory respects hints but may override based on safety thresholds.
 * 
 * <p>Example: requesting MEMORY for a 100MB payload will be overridden to FILE.
 */
public enum StoragePreference {
    /**
     * Prefer memory storage (default for small values)
     */
    MEMORY,
    
    /**
     * Prefer JSON tree storage (optimized for JsonPointer queries)
     */
    JSON,
    
    /**
     * Prefer file storage (for large or binary data)
     */
    FILE,
    
    /**
     * Let the factory auto-select based on heuristics (recommended)
     */
    AUTO
}
