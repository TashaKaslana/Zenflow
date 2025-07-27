package org.phong.zenflow.plugin.subdomain.schema.services;

import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.BaseWorkflowNode;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.plugin.PluginDefinition;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
public class SchemaTemplateStringGenerator {
    public static Set<String> generateTemplateStrings(List<BaseWorkflowNode> nodes) {
        return nodes.stream()
                .map(node -> {
                    if (node instanceof PluginDefinition pluginNode) {
                        UUID nodeId = pluginNode.getPluginNode().nodeId();
                        return nodeId.toString();
                    } else {
                        log.warn("Node does not have a valid identifier: {}", node);
                        return null;
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }
}
