package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.cluster;

import org.junit.jupiter.api.Test;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base.AiToolRegistry;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultToolRouterTest {

    static class BaseTool {}
    static class CustomTool {}

    @Test
    void resolvesBaseAndCustomTools() {
        AiToolRegistry registry = new AiToolRegistry(List.of());
        BaseTool baseTool = new BaseTool();
        registry.addTool(baseTool);

        ToolRouterConfig cfg = ToolRouterConfig.builder().build();

        DefaultToolRouter router = new DefaultToolRouter(registry, cfg, null, null);

        List<Object> tools = router.resolveTools();

        assertThat(tools).hasSize(1);
        assertThat(tools).anyMatch(t -> t instanceof BaseTool);
    }

    @Test
    void disabledRouterProducesEmptyList() {
        AiToolRegistry registry = new AiToolRegistry(List.of());
        registry.addTool(new BaseTool());

        ToolRouterConfig cfg = ToolRouterConfig.builder()
                .enabled(false)
                .build();

        DefaultToolRouter router = new DefaultToolRouter(registry, cfg, null, null);

        List<Object> tools = router.resolveTools();

        assertThat(tools).isEmpty();
    }
}
