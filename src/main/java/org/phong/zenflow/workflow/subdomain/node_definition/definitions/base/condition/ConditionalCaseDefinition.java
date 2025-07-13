package org.phong.zenflow.workflow.subdomain.node_definition.definitions.base.condition;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ConditionalCaseDefinition(@NotNull @NotBlank String when, @NotNull @NotBlank String then) {
}
