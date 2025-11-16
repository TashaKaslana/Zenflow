package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.cluster;

import lombok.Builder;
import lombok.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Declarative tool routing configuration.
 * Currently supports default packs and explicit bean names; can be extended for node-as-tool entries.
 */
@Value
@Builder
public class ToolRouterConfig {
    @Builder.Default
    boolean enabled = true;
    String defaultPack;
    @Builder.Default
    List<String> customTools = List.of();

    public static ToolRouterConfig fromRaw(Object raw) {
        if (!(raw instanceof Map<?, ?> map)) {
            return ToolRouterConfig.builder().build();
        }
        String defaultPack = map.getOrDefault("default_pack", null) instanceof String dp ? dp : null;
        List<String> custom = new ArrayList<>();
        Object rawCustom = map.get("custom_tools");
        if (rawCustom instanceof List<?> list) {
            for (Object entry : list) {
                if (entry != null) {
                    custom.add(entry.toString());
                }
            }
        }
        Boolean enabled = map.get("enabled") instanceof Boolean b ? b : Boolean.TRUE;
        return ToolRouterConfig.builder()
                .enabled(enabled)
                .defaultPack(defaultPack)
                .customTools(List.copyOf(custom))
                .build();
    }
}
