package org.phong.zenflow.plugin.subdomain.nodes.builtin.flow.branch.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ConditionalCase(@NotNull @NotBlank String when, @NotNull @NotBlank String then) {
}
