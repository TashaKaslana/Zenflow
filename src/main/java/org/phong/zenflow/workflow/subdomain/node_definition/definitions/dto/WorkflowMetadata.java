package org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record WorkflowMetadata(
        Map<String, String> aliases,
        Map<String, Set<String>> nodeDependencies,
        Map<String, OutputUsage> nodeConsumers,
        Map<String, List<String>> secrets,
        Map<String, WorkflowProfileBinding> profileAssignments,
        List<String> profileRequiredNodes
) implements Serializable {
    public WorkflowMetadata() {
        this(new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), new ArrayList<>());
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
            other.nodeConsumers != null ? new HashMap<>(other.nodeConsumers) : new HashMap<>(),
            other.secrets != null ?
                other.secrets.entrySet().stream().collect(HashMap::new,
                    (map, e) -> map.put(e.getKey(), new ArrayList<>(e.getValue())),
                    HashMap::putAll) : new HashMap<>(),
            other.profileAssignments != null ? new HashMap<>(other.profileAssignments) : new HashMap<>(),
            other.profileRequiredNodes != null ? new ArrayList<>(other.profileRequiredNodes) : new ArrayList<>()
        );
    }
}
