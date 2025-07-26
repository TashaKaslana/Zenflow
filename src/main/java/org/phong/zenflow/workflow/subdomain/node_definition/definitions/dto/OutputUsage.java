package org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * OutputUsage represents the usage of an output field in a workflow node.<br>
 * It includes the type of the output, the consumers that use this output,
 * and any aliases associated with it.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public final class OutputUsage {
    private String type;
    private Set<String> consumers = new HashSet<>();
    private List<String> alias = new ArrayList<>();
}