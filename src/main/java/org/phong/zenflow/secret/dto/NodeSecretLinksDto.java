package org.phong.zenflow.secret.dto;

import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

public record NodeSecretLinksDto(@NotNull UUID workflowId, @NotNull String nodeKey, @NotNull List<UUID> secretIds) implements Serializable {}

