package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.cluster;

import org.junit.jupiter.api.Test;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base.tools.ToolRouterConfig;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ToolRouterConfigTest {

    @Test
    void fromRaw_parsesFields() {
        Map<String, Object> raw = Map.of(
                "enabled", false,
                "default_pack", "minimal",
                "custom_tools", List.of("bean1", "bean2")
        );

        ToolRouterConfig cfg = ToolRouterConfig.fromRaw(raw);

        assertThat(cfg.isEnabled()).isFalse();
        assertThat(cfg.getDefaultPack()).isEqualTo("minimal");
        assertThat(cfg.getCustomTools()).containsExactly("bean1", "bean2");
    }

    @Test
    void fromRaw_handlesNonMap() {
        ToolRouterConfig cfg = ToolRouterConfig.fromRaw("not-a-map");
        assertThat(cfg.isEnabled()).isTrue();
        assertThat(cfg.getCustomTools()).isEmpty();
        assertThat(cfg.getDefaultPack()).isNull();
    }
}
