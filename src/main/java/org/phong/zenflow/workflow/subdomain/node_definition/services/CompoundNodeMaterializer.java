package org.phong.zenflow.workflow.subdomain.node_definition.services;

import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.workflow.subdomain.node_definition.compound.CompoundChildDescriptor;
import org.phong.zenflow.workflow.subdomain.node_definition.compound.CompoundNodeDescriptor;
import org.phong.zenflow.workflow.subdomain.node_definition.compound.CompoundNodeDescriptorProvider;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.BaseWorkflowNode;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.WorkflowDefinition;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.config.WorkflowConfig;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto.WorkflowMetadata;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.plugin.PluginNodeIdentifier;
import org.phong.zenflow.workflow.subdomain.node_definition.enums.NodeType;
import org.phong.zenflow.workflow.subdomain.node_definition.util.WorkflowNodeKeyUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Expands compound nodes that expose {@link CompoundNodeDescriptor}s into concrete child nodes.
 */
@Component
@Slf4j
public class CompoundNodeMaterializer {

    private final Map<String, CompoundNodeDescriptor> descriptorMap;

    public CompoundNodeMaterializer(List<CompoundNodeDescriptorProvider> providers) {
        this.descriptorMap = providers.stream()
                .map(CompoundNodeDescriptorProvider::descriptor)
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        CompoundNodeDescriptor::compositeKey,
                        descriptor -> descriptor,
                        (existing, ignored) -> existing
                ));
    }

    public void materialize(WorkflowDefinition definition) {
        if (definition == null || definition.nodes() == null || definition.nodes().asMap().isEmpty()) {
            return;
        }

        WorkflowMetadata metadata = definition.metadata() != null ? definition.metadata() : new WorkflowMetadata();
        Map<String, BaseWorkflowNode> nodeMap = definition.nodes().asMap();
        Set<String> managedChildren = new HashSet<>();
        Set<BaseWorkflowNode> childNodesToAdding = new HashSet<>();

        for (BaseWorkflowNode parent : nodeMap.values()) {
            PluginNodeIdentifier pluginNode = parent.getPluginNode();
            if (pluginNode == null) {
                continue;
            }

            CompoundNodeDescriptor descriptor = descriptorMap.get(pluginNode.toCacheKey());
            if (descriptor == null) {
                continue;
            }

            Set<BaseWorkflowNode> newChildren = materializeChildren(definition, managedChildren, parent, descriptor);
            if (!newChildren.isEmpty()) {
                childNodesToAdding.addAll(newChildren);
            }
        }

        definition.nodes().putAll(childNodesToAdding);

        removeOrphanChildren(definition, metadata, managedChildren);
    }

    private Set<BaseWorkflowNode> materializeChildren(WorkflowDefinition definition,
                                                      Set<String> managedChildren,
                                                      BaseWorkflowNode parent,
                                                      CompoundNodeDescriptor descriptor) {
        List<CompoundChildDescriptor> children = descriptor.childFactory().createChildren(parent);
        if (children == null) {
            children = new ArrayList<>();
        }

        List<String> childKeys = new ArrayList<>();
        Set<BaseWorkflowNode> childNodeToAdding = new HashSet<>();
        for (CompoundChildDescriptor childDescriptor : children) {
            String childKey = WorkflowNodeKeyUtils.buildChildKey(parent.getKey(), childDescriptor.childAlias());
            childKeys.add(childKey);

            BaseWorkflowNode child = ensureChildNode(definition, parent, childKey, childDescriptor);
            transferProfileBindings(parent, child, childDescriptor.profileKeys());
            managedChildren.add(childKey);

            if (definition.nodes().asMap().get(childKey) == null) {
                childNodeToAdding.add(child) ;
            }
        }

        // Preserve existing children (e.g. user-defined tools)
        if (parent.getChildNodeKeys() != null) {
            for (String existingKey : parent.getChildNodeKeys()) {
                if (!childKeys.contains(existingKey)) {
                    childKeys.add(existingKey);
                    managedChildren.add(existingKey);
                }
            }
        }

        parent.setChildNodeKeys(childKeys);

        return childNodeToAdding;
    }

    private BaseWorkflowNode ensureChildNode(WorkflowDefinition definition,
                                             BaseWorkflowNode parent,
                                             String childKey,
                                             CompoundChildDescriptor descriptor) {
        BaseWorkflowNode child = definition.nodes().asMap().get(childKey);
        if (child == null) {
            child = new BaseWorkflowNode();
            child.setKey(childKey);
            child.setType(NodeType.PLUGIN);
            child.setNext(new ArrayList<>());
            child.setMetadata(new HashMap<>());
            child.setPolicy(new HashMap<>());
            child.setConfig(new WorkflowConfig());
        }

        child.setParentNodeKey(parent.getKey());
        child.setChildNodeKeys(new ArrayList<>());

        child.setPluginNode(descriptor.pluginNodeIdentifier());
        if (child.getNext() == null) {
            child.setNext(new ArrayList<>());
        }
        if (child.getMetadata() == null) {
            child.setMetadata(new HashMap<>());
        }
        if (child.getPolicy() == null) {
            child.setPolicy(new HashMap<>());
        }

        child.setConfig(buildChildConfig(child.getConfig(), descriptor));
        return child;
    }

    private WorkflowConfig buildChildConfig(WorkflowConfig existing, CompoundChildDescriptor descriptor) {
        Map<String, Object> input = existing != null && existing.input() != null
                ? new HashMap<>(existing.input())
                : new HashMap<>();
        input.putAll(descriptor.configOverrides());

        List<String> profileKeys = descriptor.profileKeys();
        if (profileKeys == null || profileKeys.isEmpty()) {
            return new WorkflowConfig(input, existing != null ? existing.profile() : List.of());
        }
        return new WorkflowConfig(input, profileKeys);
    }

    private void transferProfileBindings(BaseWorkflowNode parent, BaseWorkflowNode child, List<String> descriptorProfiles) {
        WorkflowConfig parentConfig = parent.getConfig() != null ? parent.getConfig() : new WorkflowConfig();
        WorkflowConfig childConfig = child.getConfig() != null ? child.getConfig() : new WorkflowConfig();

        List<String> parentProfiles = parentConfig.profile();
        List<String> childProfiles = childConfig.profile();

        if (!parentProfiles.isEmpty() && childProfiles.isEmpty() && (descriptorProfiles == null || descriptorProfiles.isEmpty())) {
            child.setConfig(copyConfig(childConfig, parentProfiles));
            parent.setConfig(copyConfig(parentConfig, List.of()));
        } else {
            parent.setConfig(parentConfig);
        }
    }

    private WorkflowConfig copyConfig(WorkflowConfig source, List<String> profileKeys) {
        Map<String, Object> inputCopy = source != null && source.input() != null
                ? new HashMap<>(source.input())
                : new HashMap<>();
        Map<String, Object> outputData = source != null ? source.output() : null;
        Map<String, Object> outputCopy = (outputData != null && !outputData.isEmpty())
                ? new HashMap<>(outputData)
                : null;

        if (outputCopy != null) {
            return new WorkflowConfig(inputCopy, profileKeys, outputCopy);
        }
        return new WorkflowConfig(inputCopy, profileKeys);
    }

    private void removeOrphanChildren(WorkflowDefinition definition,
                                      WorkflowMetadata metadata,
                                      Set<String> managedChildren) {
        Map<String, BaseWorkflowNode> nodeMap = definition.nodes().asMap();
        List<String> orphans = nodeMap.values().stream()
                .filter(node -> node.getParentNodeKey() != null)
                .filter(node -> {
                    BaseWorkflowNode parent = nodeMap.get(node.getParentNodeKey());
                    if (parent == null || parent.getPluginNode() == null) {
                        return false;
                    }
                    return descriptorMap.containsKey(parent.getPluginNode().toCacheKey());
                })
                .map(BaseWorkflowNode::getKey)
                .filter(childKey -> !managedChildren.contains(childKey))
                .toList();

        for (String orphan : orphans) {
            log.debug("Removing orphaned compound child node {}", orphan);
            definition.nodes().remove(orphan);
            cleanupMetadata(metadata, orphan);
        }
    }

    private void cleanupMetadata(WorkflowMetadata metadata, String nodeKey) {
        if (metadata == null) {
            return;
        }
        metadata.nodeDependencies().remove(nodeKey);
        metadata.profileAssignments().remove(nodeKey);
        metadata.profileRequiredNodes().removeIf(nodeKey::equals);
        metadata.secrets().values().forEach(nodes -> nodes.remove(nodeKey));
        metadata.nodeConsumers().values().forEach(usage -> usage.getConsumers().remove(nodeKey));
    }

}
