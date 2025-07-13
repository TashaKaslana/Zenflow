package org.phong.zenflow.workflow.subdomain.node_definition.definitions.base.loop;

import lombok.Getter;
import lombok.Setter;
import org.phong.zenflow.workflow.subdomain.node_definition.enums.NodeType;

import java.util.List;
import java.util.Map;

@Getter
@Setter
public final class ForLoopDefinition extends BaseLoopDefinition {
    private final String iterator;

    private final String itemVar;

    public ForLoopDefinition(String key, NodeType type, List<String> next, Map<String, Object> config,
                             Map<String, Object> metadata, Map<String, Object> policy, String loopType,
                             List<String> loopEnd, String iterator, String itemVar) {
        super(key, type, next, config, metadata, policy, loopType, loopEnd);
        this.iterator = iterator;
        this.itemVar = itemVar;
    }
}
