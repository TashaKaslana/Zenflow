package org.phong.zenflow.secret.dto;

import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public record AggregatedSecretSetupDto(
        @NotNull Map<String, String> secrets,
        @NotNull Map<String, Map<String, String>> profiles,
        @NotNull Map<String, String> nodeProfiles,
        @NotNull Map<String, List<String>> nodeSecrets
) implements Serializable {}

