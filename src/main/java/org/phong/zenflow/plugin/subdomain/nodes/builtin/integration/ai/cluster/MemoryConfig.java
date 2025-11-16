package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.cluster;

import lombok.Builder;
import lombok.Value;

import java.util.Map;

/**
 * Declarative memory configuration for the cluster.
 */
@Value
@Builder(toBuilder = true)
public class MemoryConfig {
    public enum Backend {
        IN_MEMORY,
        CONTEXT,
        KV,
        VECTOR,
        EXTERNAL
    }

    @Builder.Default
    Backend backend = Backend.CONTEXT;
    String key;
    String namespace;
    @Builder.Default
    int maxHistoryMessages = 10;
    @Builder.Default
    boolean persistent = true;
    Map<String, Object> options;

    public static MemoryConfig fromRaw(Object raw) {
        if (!(raw instanceof Map<?, ?> map)) {
            return MemoryConfig.builder().build();
        }
        Backend backend = Backend.CONTEXT;
        Object backendVal = map.get("backend");
        if (backendVal instanceof String backendStr) {
            try {
                backend = Backend.valueOf(backendStr.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
                // fallback to default
            }
        }
        String key = map.get("key") instanceof String k ? k : null;
        String namespace = map.get("namespace") instanceof String ns ? ns : null;
        int maxHistory = map.get("max_history_messages") instanceof Number n ? n.intValue() : 10;
        boolean persistent = map.get("persistent") instanceof Boolean b ? b : true;

        @SuppressWarnings("unchecked")
        Map<String, Object> opts = map.get("options") instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();

        return MemoryConfig.builder()
                .backend(backend)
                .key(key)
                .namespace(namespace)
                .maxHistoryMessages(maxHistory)
                .persistent(persistent)
                .options(opts)
                .build();
    }
}
