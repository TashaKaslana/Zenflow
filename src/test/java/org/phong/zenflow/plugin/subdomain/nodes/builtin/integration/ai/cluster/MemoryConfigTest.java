package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.cluster;

import org.junit.jupiter.api.Test;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base.memory.MemoryConfig;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class MemoryConfigTest {

    @Test
    void fromRaw_parsesFields() {
        Map<String, Object> raw = Map.of(
                "backend", "kv",
                "key", "session-1",
                "namespace", "ns",
                "max_history_messages", 5,
                "persistent", false
        );

        MemoryConfig cfg = MemoryConfig.fromRaw(raw);

        assertThat(cfg.getBackend()).isEqualTo(MemoryConfig.Backend.KV);
        assertThat(cfg.getKey()).isEqualTo("session-1");
        assertThat(cfg.getNamespace()).isEqualTo("ns");
        assertThat(cfg.getMaxHistoryMessages()).isEqualTo(5);
        assertThat(cfg.isPersistent()).isFalse();
    }

    @Test
    void fromRaw_defaultsWhenInvalid() {
        MemoryConfig cfg = MemoryConfig.fromRaw("not-a-map");
        assertThat(cfg.getBackend()).isEqualTo(MemoryConfig.Backend.CONTEXT);
        assertThat(cfg.getMaxHistoryMessages()).isEqualTo(10);
        assertThat(cfg.isPersistent()).isTrue();
    }
}
