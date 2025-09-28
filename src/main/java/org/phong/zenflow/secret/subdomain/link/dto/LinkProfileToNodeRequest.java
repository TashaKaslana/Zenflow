package org.phong.zenflow.secret.subdomain.link.dto;

import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.util.UUID;

public record LinkProfileToNodeRequest(@NotNull UUID profileId, @NotNull String nodeKey) implements Serializable {
}

