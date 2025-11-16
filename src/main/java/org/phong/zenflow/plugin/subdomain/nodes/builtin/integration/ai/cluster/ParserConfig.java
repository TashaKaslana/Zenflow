package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.cluster;

import lombok.Builder;
import lombok.Value;

import java.util.Map;

@Value
@Builder(toBuilder = true)
public class ParserConfig {
    public enum Strategy {
        AUTO, TEXT, JSON, SCHEMA, RAW
    }

    @Builder.Default
    Strategy strategy = Strategy.AUTO;
    @Builder.Default
    String onError = "fail"; // fail | fallback_to_text
    @Builder.Default
    int maxRetries = 0;
    Map<String, Object> options;

    public static ParserConfig fromRaw(Object raw) {
        if (!(raw instanceof Map<?, ?> map)) {
            return ParserConfig.builder().build();
        }
        Strategy strategy = Strategy.AUTO;
        Object strategyVal = map.get("strategy");
        if (strategyVal instanceof String s) {
            try {
                strategy = Strategy.valueOf(s.trim().toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }
        Object onErrorRaw = map.containsKey("on_error") ? map.get("on_error") : "fail";
        String onError = onErrorRaw != null ? onErrorRaw.toString() : "fail";
        int maxRetries = map.get("max_retries") instanceof Number n ? n.intValue() : 0;
        @SuppressWarnings("unchecked")
        Map<String, Object> options = map.get("options") instanceof Map<?, ?> m ? (Map<String, Object>) m : Map.of();

        return ParserConfig.builder()
                .strategy(strategy)
                .onError(onError)
                .maxRetries(maxRetries)
                .options(options)
                .build();
    }
}
