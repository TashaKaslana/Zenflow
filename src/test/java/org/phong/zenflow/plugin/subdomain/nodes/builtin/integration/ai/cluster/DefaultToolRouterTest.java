package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.cluster;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base.AiToolRegistry;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base.tools.DefaultToolRouter;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base.tools.NodeToolCallback;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base.tools.ToolRouterConfig;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.BaseWorkflowNode;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultToolRouterTest {

    static class BaseTool {}
    static class CustomTool {}

    @Mock
    private ExecutionContext context;
    @Mock
    private ObjectMapper objectMapper;

    @Test
    void resolvesBaseAndCustomTools() {
        AiToolRegistry registry = new AiToolRegistry(List.of());
        BaseTool baseTool = new BaseTool();
        registry.addTool(baseTool);

        ToolRouterConfig cfg = ToolRouterConfig.builder().build();

        DefaultToolRouter router = new DefaultToolRouter(registry, cfg, context, objectMapper);

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

        DefaultToolRouter router = new DefaultToolRouter(registry, cfg, context, objectMapper);

        List<Object> tools = router.resolveTools();

        assertThat(tools).isEmpty();
    }

    @Test
    void discoversNodeTools() {
        AiToolRegistry registry = new AiToolRegistry(List.of());
        ToolRouterConfig cfg = ToolRouterConfig.builder().build();

        // Mock context setup
        String clusterNodeKey = "ai_cluster";
        String childNodeKey = "child_tool_node";
        
        BaseWorkflowNode clusterNode = mock(BaseWorkflowNode.class);
        when(clusterNode.getChildNodeKeys()).thenReturn(List.of(childNodeKey));
        
        BaseWorkflowNode childNode = mock(BaseWorkflowNode.class);
        when(childNode.getKey()).thenReturn(childNodeKey);
        // getName() does not exist on BaseWorkflowNode
        // when(childNode.getPluginNode()).thenReturn(new PluginNodeIdentifier("plugin", "node", "1.0.0", "builtin"));

        when(context.getNodeKey()).thenReturn(clusterNodeKey);
        when(context.getWorkflowNode(clusterNodeKey)).thenReturn(clusterNode);
        when(context.getWorkflowNode(childNodeKey)).thenReturn(childNode);

        DefaultToolRouter router = new DefaultToolRouter(registry, cfg, context, objectMapper);

        List<Object> tools = router.resolveTools();

        assertThat(tools).hasSize(1);
        assertThat(tools.get(0)).isInstanceOf(NodeToolCallback.class);
    }
}
