package org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public record WorkflowMetadata(Map<String, String> aliases, Map<String, Set<String>> nodeDependencies, Map<String, OutputUsage> nodeConsumers) {
    public WorkflowMetadata() {
        this(new HashMap<>(), new HashMap<>(), new HashMap<>());
    }

    /**
     * Deep copy constructor
     */
    public WorkflowMetadata(WorkflowMetadata other) {
        this(
            other.aliases != null ? new HashMap<>(other.aliases) : new HashMap<>(),
            other.nodeDependencies != null ?
                other.nodeDependencies.entrySet().stream()
                    .collect(HashMap::new,
                        (map, entry) -> map.put(entry.getKey(), new HashSet<>(entry.getValue())),
                        HashMap::putAll) : new HashMap<>(),
            other.nodeConsumers != null ? new HashMap<>(other.nodeConsumers) : new HashMap<>()
        );
    }
}
