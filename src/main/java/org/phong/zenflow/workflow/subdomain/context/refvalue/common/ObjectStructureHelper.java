package org.phong.zenflow.workflow.subdomain.context.refvalue.common;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.core.utils.ObjectConversion;

import java.util.Map;

@Slf4j
public class ObjectStructureHelper {
    /**
     * Estimates size of an object in bytes (rough approximation).
     */
    public static long estimateSize(Object value, long fallbackSize) {
        switch (value) {
            case null -> {
                return 0;
            }
            case String s -> {
                return s.length() * 2L; // UTF-16
            }
            case byte[] b -> {
                return b.length;
            }
            case Number ignored -> {
                return 8;
            }
            case Boolean ignored -> {
                return 1;
            }
            default -> {
            }
        }

        // For complex objects, serialize to estimate
        try {
            byte[] serialized = ObjectConversion.getObjectMapper().writeValueAsBytes(value);
            return serialized.length;
        } catch (Exception e) {
            log.debug("Could not estimate size for {}, assuming small", value.getClass(), e);
            return fallbackSize;
        }
    }

    /**
     * Checks if value is likely JSON-structured data.
     */
    public static boolean isJsonStructure(Object value, String mediaType) {
        if (mediaType != null && mediaType.contains("json")) {
            return true;
        }
        if (value instanceof Map || value instanceof JsonNode) {
            return true;
        }
        if (value instanceof String str) {
            String trimmed = str.trim();
            return (trimmed.startsWith("{") && trimmed.endsWith("}")) ||
                    (trimmed.startsWith("[") && trimmed.endsWith("]"));
        }
        return false;
    }
}
