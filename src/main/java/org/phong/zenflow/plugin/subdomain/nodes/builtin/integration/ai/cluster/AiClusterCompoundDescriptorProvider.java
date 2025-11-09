package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.cluster;

import lombok.RequiredArgsConstructor;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base.dto.AiClusterConfig;
import org.phong.zenflow.workflow.subdomain.node_definition.compound.CompoundChildDescriptor;
import org.phong.zenflow.workflow.subdomain.node_definition.compound.CompoundChildFactory;
import org.phong.zenflow.workflow.subdomain.node_definition.compound.CompoundNodeDescriptor;
import org.phong.zenflow.workflow.subdomain.node_definition.compound.CompoundNodeDescriptorProvider;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.plugin.PluginNodeIdentifier;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

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
            String provider = config.getModel() != null ? config.getModel() : "gemini";
            var providerInfo = resolver.resolve(provider).orElse(resolver.defaultProvider());
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
}
