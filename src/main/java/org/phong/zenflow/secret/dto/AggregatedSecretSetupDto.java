package org.phong.zenflow.secret.dto;

import jakarta.validation.constraints.NotNull;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Aggregated view of secrets and profiles within a workflow.
 * <p>
 * All identifiers are stable and collision-free:
 * - profiles are keyed by profileId
 * - secrets are keyed by secretId
 * - nodeProfiles maps nodeKey -> profileId
 * - nodeSecrets maps nodeKey -> [secretId]
 * <p>
 * For display purposes, clients can map profileId -> profileName using profileNames.
 */
public record AggregatedSecretSetupDto(
        @NotNull Map<String, String> secrets,
        @NotNull Map<String, Map<String, String>> profiles,
        @NotNull Map<String, String> nodeProfiles,
        @NotNull Map<String, List<String>> nodeSecrets,
        @NotNull Map<String, String> profileNames,
        @NotNull Map<String, String> secretKeys
) implements Serializable {}
