package org.phong.zenflow.secret.dto;

import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.util.UUID;

public record NodeProfileLinkDto(@NotNull UUID workflowId, @NotNull String nodeKey, @NotNull UUID profileId) implements Serializable {}

