package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.cluster;

import org.junit.jupiter.api.Test;
import org.phong.zenflow.workflow.subdomain.node_definition.compound.CompoundChildDescriptor;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.BaseWorkflowNode;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.config.WorkflowConfig;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.plugin.PluginNodeIdentifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiClusterCompoundDescriptorProviderTest {

    @Test
    void overridesChildPluginsWhenConfigured() {
        AiClusterCompoundDescriptorProvider provider = new AiClusterCompoundDescriptorProvider(new AiClusterProviderResolver());

        Map<String, Object> input = new HashMap<>();
        input.put("prompt", "Hello");
        input.put("provider", "gemini");
        input.put("child_nodes", Map.of(
                "tools", "custom:tools.pack:2.1.0",
                "context", "custom:memory.store:3.0.0",
                "parser", "custom:parser.json:4.0.0"
        ));
        input.put("child_executor_types", Map.of(
                "parser", "remote"
        ));

        BaseWorkflowNode parent = new BaseWorkflowNode();
        parent.setKey("cluster-node");
        parent.setPluginNode(new PluginNodeIdentifier("core", "ai.cluster", "1.0.0", "builtin"));
        parent.setConfig(new WorkflowConfig(input));
        parent.setChildNodeKeys(new ArrayList<>());

        List<CompoundChildDescriptor> children = provider.descriptor().childFactory().createChildren(parent);

        Map<String, PluginNodeIdentifier> identifiersByAlias = children.stream()
                .collect(Collectors.toMap(
                        CompoundChildDescriptor::childAlias,
                        CompoundChildDescriptor::pluginNodeIdentifier,
                        (left, right) -> left));

        assertEquals("custom:tools.pack:2.1.0", identifiersByAlias.get("tools").toString());
        assertEquals("custom:memory.store:3.0.0", identifiersByAlias.get("context").toString());
        assertEquals("custom:parser.json:4.0.0", identifiersByAlias.get("parser").toString());
        assertEquals("remote", identifiersByAlias.get("parser").getExecutorType());
    }

    @Test
    void fallsBackToDefaultWhenOverrideInvalid() {
        AiClusterCompoundDescriptorProvider provider = new AiClusterCompoundDescriptorProvider(new AiClusterProviderResolver());

        Map<String, Object> input = new HashMap<>();
        input.put("prompt", "Hello");
        input.put("provider", "gemini");
        input.put("child_nodes", Map.of(
                "context", "invalid_format"
        ));

        BaseWorkflowNode parent = new BaseWorkflowNode();
        parent.setKey("cluster-node");
        parent.setPluginNode(new PluginNodeIdentifier("core", "ai.cluster", "1.0.0", "builtin"));
        parent.setConfig(new WorkflowConfig(input));
        parent.setChildNodeKeys(new ArrayList<>());

        List<CompoundChildDescriptor> children = provider.descriptor().childFactory().createChildren(parent);

        Map<String, PluginNodeIdentifier> identifiersByAlias = children.stream()
                .collect(Collectors.toMap(
                        CompoundChildDescriptor::childAlias,
                        CompoundChildDescriptor::pluginNodeIdentifier,
                        (left, right) -> left));

        assertTrue(identifiersByAlias.containsKey("context"));
        assertEquals("core:context_variable:1.0.0", identifiersByAlias.get("context").toString());
    }
}
