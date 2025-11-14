package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.cluster;

import lombok.RequiredArgsConstructor;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base.dto.AiClusterConfig;
import org.phong.zenflow.workflow.subdomain.node_definition.compound.CompoundChildDescriptor;
import org.phong.zenflow.workflow.subdomain.node_definition.compound.CompoundChildFactory;
import org.phong.zenflow.workflow.subdomain.node_definition.compound.CompoundNodeDescriptor;
import org.phong.zenflow.workflow.subdomain.node_definition.compound.CompoundNodeDescriptorProvider;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.BaseWorkflowNode;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.plugin.PluginNodeIdentifier;
import org.phong.zenflow.workflow.subdomain.node_definition.util.WorkflowNodeKeyUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AiClusterCompoundDescriptorProvider implements CompoundNodeDescriptorProvider {

    private final AiClusterProviderResolver resolver;

    @Override
    public CompoundNodeDescriptor descriptor() {
        return new CompoundNodeDescriptor("core:ai.cluster:1.0.0", buildFactory());
    }

    private CompoundChildFactory buildFactory() {
        return parent -> {
            AiClusterConfig config = AiClusterConfig.fromNodeConfig(parent.getConfig());
            var providerInfo = determineProvider(parent, config);
            PluginNodeIdentifier pluginNodeIdentifier = new PluginNodeIdentifier(
                    null,
                    providerInfo.pluginKey(),
                    providerInfo.nodeKey(),
                    providerInfo.version(),
                    providerInfo.executorType()
            );

            CompoundChildDescriptor descriptor = new CompoundChildDescriptor(
                    providerInfo.childAlias(),
                    pluginNodeIdentifier,
                    Map.of(),
                    List.of()
            );
            return List.of(descriptor);
        };
    }

    private AiClusterProviderResolver.ProviderInfo determineProvider(BaseWorkflowNode parent, AiClusterConfig config) {
        return resolveFromExistingChild(parent)
                .or(() -> resolver.resolve(config.getModel()))
                .orElse(resolver.defaultProvider());
    }

    private Optional<AiClusterProviderResolver.ProviderInfo> resolveFromExistingChild(BaseWorkflowNode parent) {
        List<String> childKeys = parent.getChildNodeKeys();
        if (childKeys == null || childKeys.isEmpty()) {
            return Optional.empty();
        }
        return childKeys.stream()
                .map(WorkflowNodeKeyUtils::extractChildAlias)
                .filter(Objects::nonNull)
                .map(resolver::resolveByChildAlias)
                .flatMap(Optional::stream)
                .findFirst();
    }
}
