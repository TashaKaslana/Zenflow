package org.phong.zenflow.secret.dto;

import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.util.UUID;

public record LinkSecretToNodeRequest(@NotNull UUID secretId, @NotNull String nodeKey) implements Serializable {
}

