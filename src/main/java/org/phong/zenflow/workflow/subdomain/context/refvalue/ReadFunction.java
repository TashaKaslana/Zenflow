package org.phong.zenflow.workflow.subdomain.context.refvalue;

import java.io.IOException;

/**
 * Functional interface for selective extraction from a RefValue without
 * forcing full materialization of the entire payload.
 * 
 * <p>The function receives a RefValueAccess helper that provides backend-specific
 * optimizations (streaming JSON parsing, byte range access, etc.).
 * 
 * <p>Example usage:
 * <pre>{@code
 * String name = refValue.read(access -> 
 *     access.asObject(Map.class).get("user").get("name")
 * );
 * }</pre>
 * 
 * @param <R> the type of result extracted from the value
 */
@FunctionalInterface
public interface ReadFunction<R> {
    
    /**
     * Applies this function to extract a result from the given access helper.
     * 
     * @param access helper providing backend-optimized access methods
     * @return the extracted result
     * @throws IOException if reading or deserialization fails
     */
    R apply(RefValueAccess access) throws IOException;
}
