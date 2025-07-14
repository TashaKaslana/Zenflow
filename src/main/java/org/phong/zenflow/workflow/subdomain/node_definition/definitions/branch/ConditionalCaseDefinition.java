package org.phong.zenflow.workflow.subdomain.node_definition.definitions.branch;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ConditionalCaseDefinition(@NotNull @NotBlank String when, @NotNull @NotBlank String then) {
}
