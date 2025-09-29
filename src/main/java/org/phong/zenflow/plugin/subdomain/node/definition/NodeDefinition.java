package org.phong.zenflow.plugin.subdomain.node.definition;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.phong.zenflow.plugin.subdomain.node.definition.aspect.NodeExecutor;
import org.phong.zenflow.plugin.subdomain.node.definition.aspect.NodeValidator;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
public class NodeDefinition {
    NodeExecutor nodeExecutor;
    NodeValidator nodeValidator;

    String name;
    String description;
    String icon;
    String type;
    String[] tags;
}
