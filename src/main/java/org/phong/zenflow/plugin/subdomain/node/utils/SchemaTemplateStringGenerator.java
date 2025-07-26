package org.phong.zenflow.plugin.subdomain.node.utils;

import org.phong.zenflow.workflow.subdomain.node_definition.definitions.BaseWorkflowNode;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.plugin.PluginDefinition;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class SchemaTemplateStringGenerator {
    public static List<String> generateTemplateStrings(List<BaseWorkflowNode> nodes) {
        return nodes.stream()
                .map(node -> {
                    if (node instanceof PluginDefinition pluginNode) {
                        UUID nodeId = pluginNode.getPluginNode().nodeId();
                        return nodeId.toString();
                    } else {
                        throw new IllegalArgumentException("Node does not have a valid identifier: " + node);
                    }
                })
                .collect(Collectors.toList());
    }
}
