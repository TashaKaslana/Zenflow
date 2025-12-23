package org.phong.zenflow.plugin.subdomain.node.definition.aspect;

import java.util.Set;

public record NodeState(boolean isDiscoverable, Set<NodeStateType> stateTypes) {
    public NodeState() {
        this(true, Set.of(NodeStateType.NORMAL));
    }

    public NodeState(Set<NodeStateType> stateTypes) {
        this(true, stateTypes);
    }
}
