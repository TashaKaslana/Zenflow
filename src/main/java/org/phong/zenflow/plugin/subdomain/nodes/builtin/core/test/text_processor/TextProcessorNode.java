package org.phong.zenflow.plugin.subdomain.nodes.builtin.core.test.text_processor;

import lombok.AllArgsConstructor;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinition;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinitionProvider;
import org.phong.zenflow.plugin.subdomain.node.registry.PluginNode;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@PluginNode(
        key = "test:text.process",
        name = "Text Processor",
        version = "1.0.0"
)
public class TextProcessorNode implements NodeDefinitionProvider {
    private final TextProcessorExecutor executor;

    @Override
    public NodeDefinition definition() {
        return NodeDefinition.builder()
                .nodeExecutor(executor)
                .build();
    }
}
