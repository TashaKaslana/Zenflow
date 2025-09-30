package org.phong.zenflow.plugin.subdomain.nodes.builtin.core.test.number_validator;

import lombok.AllArgsConstructor;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinition;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinitionProvider;
import org.phong.zenflow.plugin.subdomain.node.registry.PluginNode;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@PluginNode(
        key = "test:number.validate",
        name = "Number Validator",
        version = "1.0.0"
)
public class NumberValidatorNode implements NodeDefinitionProvider {
    private final NumberValidatorExecutor executor;

    @Override
    public NodeDefinition definition() {
        return NodeDefinition.builder()
                .nodeExecutor(executor)
                .build();
    }
}
