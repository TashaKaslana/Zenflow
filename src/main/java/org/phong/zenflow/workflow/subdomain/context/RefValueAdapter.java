package org.phong.zenflow.workflow.subdomain.context;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.workflow.subdomain.context.refvalue.RefValue;
import org.phong.zenflow.workflow.subdomain.context.refvalue.RefValueFactory;
import org.phong.zenflow.workflow.subdomain.context.refvalue.StoragePreference;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Adapter layer between RuntimeContext and RefValue system.
 * Provides backward-compatible conversion between Object and RefValue.
 * 
 * <p>This layer allows gradual migration:
 * <ul>
 *   <li>Legacy code continues using Object-based APIs</li>
 *   <li>New code can use RefValue-based APIs for streaming/optimization</li>
 *   <li>RuntimeContext internally manages RefValues</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RefValueAdapter {
    
    private final RefValueFactory factory;
    
    /**
     * Converts an Object to a RefValue using auto-selection heuristics.
     * 
     * @param value object to wrap
     * @return RefValue instance
     */
    public RefValue toRefValue(Object value) {
        return factory.create(value, StoragePreference.AUTO, null);
    }
    
    /**
     * Converts a RefValue back to an Object (for backward compatibility).
     * This may trigger full materialization of file-backed values.
     * 
     * @param refValue the RefValue to materialize
     * @return plain object, or null
     */
    public Object fromRefValue(RefValue refValue) {
        if (refValue == null) {
            return null;
        }
        
        try {
            return refValue.read(Object.class);
        } catch (IOException e) {
            log.error("Failed to materialize RefValue", e);
            return null;
        }
    }
}
