package org.phong.zenflow.workflow.subdomain.node_definition.definitions.navigator;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class NodeExecutorRegistry {
    private final Map<String, NodeExecutor<?>> executors;

    public NodeExecutorRegistry(List<NodeExecutor<?>> executors) {
        this.executors = executors.stream().collect(Collectors.toMap(NodeExecutor::getNodeType, map -> map));
    }

    public Optional<NodeExecutor<?>> getExecutor(String type) {
        return Optional.ofNullable(executors.get(type));
    }
}
