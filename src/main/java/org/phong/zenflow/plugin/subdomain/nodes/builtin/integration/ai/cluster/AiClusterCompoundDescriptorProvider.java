package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.cluster;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.ai.base.dto.AiClusterConfig;
import org.phong.zenflow.workflow.subdomain.node_definition.compound.CompoundChildDescriptor;
import org.phong.zenflow.workflow.subdomain.node_definition.compound.CompoundChildFactory;
import org.phong.zenflow.workflow.subdomain.node_definition.compound.CompoundNodeDescriptor;
import org.phong.zenflow.workflow.subdomain.node_definition.compound.CompoundNodeDescriptorProvider;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.BaseWorkflowNode;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.plugin.PluginNodeIdentifier;
import org.phong.zenflow.workflow.subdomain.node_definition.util.WorkflowNodeKeyUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Component
@RequiredArgsConstructor
@Slf4j
public class AiClusterCompoundDescriptorProvider implements CompoundNodeDescriptorProvider {

    private final AiClusterProviderResolver resolver;

    private static final PluginNodeIdentifier DEFAULT_TOOL_NODE_IDENTIFIER =
        new PluginNodeIdentifier("core", "ai.cluster.tools", "1.0.0", "builtin");
    private static final PluginNodeIdentifier DEFAULT_CONTEXT_NODE_IDENTIFIER =
        new PluginNodeIdentifier("core", "context_variable", "1.0.0", "builtin");
    private static final PluginNodeIdentifier DEFAULT_PARSER_NODE_IDENTIFIER =
        new PluginNodeIdentifier("core", "ai.output_parser", "1.0.0", "builtin");

    private static final String CHILD_ALIAS_TOOLS = "tools";
    private static final String CHILD_ALIAS_CONTEXT = "context";
    private static final String CHILD_ALIAS_PARSER = "parser";

    @Override
    public CompoundNodeDescriptor descriptor() {
        return new CompoundNodeDescriptor("core:ai.cluster:1.0.0", buildFactory());
    }

    private CompoundChildFactory buildFactory() {
        return parent -> {
            AiClusterConfig config = AiClusterConfig.fromNodeConfig(parent.getConfig());
            var providerInfo = determineProvider(parent, config);
        List<CompoundChildDescriptor> descriptors = new ArrayList<>();

        descriptors.add(buildToolsDescriptor(parent, config));
        descriptors.add(buildContextDescriptor(parent, config));
        descriptors.add(buildParserDescriptor(parent, config));
        descriptors.add(buildProviderDescriptor(providerInfo));

        return descriptors;
        };
    }

    private CompoundChildDescriptor buildToolsDescriptor(BaseWorkflowNode parent, AiClusterConfig config) {
        PluginNodeIdentifier identifier = resolveChildPlugin(CHILD_ALIAS_TOOLS, config, DEFAULT_TOOL_NODE_IDENTIFIER);
        return new CompoundChildDescriptor(
            CHILD_ALIAS_TOOLS,
            identifier,
            Map.of(),
            parent.getConfig() != null ? parent.getConfig().profile() : List.of()
        );
    }

    private CompoundChildDescriptor buildContextDescriptor(BaseWorkflowNode parent, AiClusterConfig config) {
        Map<String, Object> overrides = new HashMap<>();
        if (config.getMemoryKey() != null) {
            overrides.put("key", config.getMemoryKey());
        }
        overrides.put("include_history", config.isIncludeHistory());
        overrides.put("max_history_messages", config.getMaxHistoryMessages());

        PluginNodeIdentifier identifier = resolveChildPlugin(CHILD_ALIAS_CONTEXT, config, DEFAULT_CONTEXT_NODE_IDENTIFIER);

        return new CompoundChildDescriptor(
            CHILD_ALIAS_CONTEXT,
            identifier,
            overrides,
            parent.getConfig() != null ? parent.getConfig().profile() : List.of()
        );
    }

    private CompoundChildDescriptor buildParserDescriptor(BaseWorkflowNode parent, AiClusterConfig config) {
        Map<String, Object> overrides = Map.of(
            "strategy", Optional.ofNullable(config.getOutputParser()).orElse("auto"),
            "response_format", Optional.ofNullable(config.getResponseFormat()).orElse("text")
        );
        PluginNodeIdentifier identifier = resolveChildPlugin(CHILD_ALIAS_PARSER, config, DEFAULT_PARSER_NODE_IDENTIFIER);
        return new CompoundChildDescriptor(
            CHILD_ALIAS_PARSER,
            identifier,
            overrides,
            parent.getConfig() != null ? parent.getConfig().profile() : List.of()
        );
    }

    private CompoundChildDescriptor buildProviderDescriptor(AiClusterProviderResolver.ProviderInfo providerInfo) {
    PluginNodeIdentifier pluginNodeIdentifier = new PluginNodeIdentifier(
        providerInfo.pluginKey(),
        providerInfo.nodeKey(),
        providerInfo.version(),
        providerInfo.executorType()
    );
    return new CompoundChildDescriptor(
        providerInfo.childAlias(),
        pluginNodeIdentifier,
        Map.of(),
        List.of()
    );
    }

    private AiClusterProviderResolver.ProviderInfo determineProvider(BaseWorkflowNode parent, AiClusterConfig config) {
        return resolveFromExistingChild(parent)
                .or(() -> resolver.resolve(config.getModel()))
                .orElse(resolver.defaultProvider());
    }

    private PluginNodeIdentifier resolveChildPlugin(String childAlias,
                                                    AiClusterConfig config,
                                                    PluginNodeIdentifier defaultIdentifier) {
        String override = config.getChildNodes().get(childAlias);
        if (override == null || override.isBlank()) {
            return defaultIdentifier;
        }

        String executorType = config.getChildExecutorTypes().getOrDefault(childAlias, defaultIdentifier.getExecutorType());
        try {
            return PluginNodeIdentifier.fromString(override, executorType);
        } catch (IllegalArgumentException ex) {
            log.warn("Invalid plugin identifier '{}' configured for AI cluster child '{}'. Using default {}", override, childAlias, defaultIdentifier, ex);
            return defaultIdentifier;
        }
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
