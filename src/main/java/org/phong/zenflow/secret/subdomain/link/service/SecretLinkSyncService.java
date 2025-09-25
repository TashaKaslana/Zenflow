package org.phong.zenflow.secret.subdomain.link.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.secret.service.SecretService;
import org.phong.zenflow.secret.subdomain.link.dto.LinkProfileToNodeRequest;
import org.phong.zenflow.secret.subdomain.link.event.SecretLinkedEvent;
import org.phong.zenflow.workflow.infrastructure.persistence.repository.WorkflowRepository;
import org.phong.zenflow.workflow.subdomain.schema_validator.dto.ValidationError;
import org.phong.zenflow.workflow.subdomain.schema_validator.dto.ValidationResult;
import org.phong.zenflow.workflow.subdomain.schema_validator.enums.ValidationErrorCode;
import java.time.OffsetDateTime;
import org.phong.zenflow.secret.subdomain.link.dto.SecretNodeLinkInsertRequestDto;
import org.phong.zenflow.secret.subdomain.link.infrastructure.projection.SecretNodeLinkInfo;
import org.phong.zenflow.secret.subdomain.link.infrastructure.projection.SecretProfileNodeLinkInfo;
import org.phong.zenflow.secret.subdomain.profile.service.ProfileSecretService;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.BaseWorkflowNode;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.WorkflowDefinition;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto.WorkflowProfileBinding;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SecretLinkSyncService {
    private final SecretService secretService;
    private final SecretLinkService secretLinkService;
    private final ProfileSecretService profileSecretService;
    private final WorkflowRepository workflowRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void syncLinksFromMetadata(UUID workflowId, WorkflowDefinition definition) {
        if (definition == null || definition.metadata() == null) return;
        try {
            syncSecretsFromMetadata(workflowId, definition);
        } catch (Exception e) {
            log.warn("Secret auto-linking from metadata.secrets failed during upsert: {}", e.getMessage());
        }

        // Unified profile sync - handles both metadata-driven and node-config-driven profiles
        try {
            syncAllProfiles(workflowId, definition);
        } catch (Exception e) {
            log.warn("Profile auto-linking failed during upsert: {}", e.getMessage());
        }

        eventPublisher.publishEvent(new SecretLinkedEvent(definition, workflowId));
    }

    private void syncSecretsFromMetadata(UUID workflowId, WorkflowDefinition definition) {
        if (definition.metadata().secrets() == null) return;

        Map<String, List<UUID>> idsByKey = secretService.getSecretIdsByKey(workflowId);

        // nodeKey -> desired secretIds
        Map<String, Set<UUID>> desired = new HashMap<>();
        definition.metadata().secrets().forEach((secretKey, nodes) -> {
            List<UUID> ids = idsByKey.getOrDefault(secretKey, List.of());
            for (String nk : nodes) {
                desired.computeIfAbsent(nk, k -> new HashSet<>()).addAll(ids);
            }
        });

        //In metadata there are no secrets, so remove all links
        if (desired.isEmpty()) {
            secretLinkService.unlinkSecretsByWorkflowId(workflowId);
        }

        List<SecretNodeLinkInfo> secretIdsByWorkflowId = secretLinkService.getLinkedSecretIdsByWorkflowId(workflowId);
        Map<String, Map<UUID, UUID>> secretIdsMap = secretIdsByWorkflowId.stream().collect(Collectors.groupingBy(
                SecretNodeLinkInfo::getNodeKey,
                Collectors.collectingAndThen(
                        Collectors.toList(),
                        list -> list.stream()
                                .collect(Collectors.toMap(
                                        SecretNodeLinkInfo::getSecretId,
                                        SecretNodeLinkInfo::getId,
                                        (current, duplicate) -> duplicate
                                ))
                )
        ));

        List<SecretNodeLinkInsertRequestDto> insertButchDto = new ArrayList<>();
        Set<UUID> idsToDelete = new HashSet<>();

        populateIdListForInsertAndRemove(desired, secretIdsMap, insertButchDto, idsToDelete);

        filterRedundantIdFromDb(secretIdsMap, desired, idsToDelete);

        secretLinkService.unlinkSecretByWorkflowIdAndSecretIds(idsToDelete);
        secretLinkService.linkSecretsButchToNode(workflowId, insertButchDto);
    }

    private static void populateIdListForInsertAndRemove(Map<String, Set<UUID>> desired, Map<String, Map<UUID, UUID>> secretIdsMap, List<SecretNodeLinkInsertRequestDto> idsToInsertMissingSecret, Set<UUID> idsToDelete) {
        for (var entry : desired.entrySet()) {
            String nk = entry.getKey();
            Set<UUID> want = entry.getValue();
            Map<UUID, UUID> infoMap = secretIdsMap.get(nk);

            // If no links exist, add all wanted
            if (infoMap == null || infoMap.isEmpty()) {
                if (want.isEmpty()) continue;

                idsToInsertMissingSecret.addAll(
                        want.stream()
                                .map(id -> new SecretNodeLinkInsertRequestDto(id, nk))
                                .toList()
                );

                continue;
            }
            List<UUID> haveList = infoMap.keySet().stream().toList();

            // Add missing
            Set<UUID> have = new HashSet<>(haveList);
            for (UUID id : want) {
                if (!have.contains(id)) {
                    idsToInsertMissingSecret.add(new SecretNodeLinkInsertRequestDto(id, nk));
                }
            }

            // Remove extras not declared
            for (UUID secretId : have) {
                if (!want.contains(secretId)) {
                    UUID id = infoMap.get(secretId);
                    idsToDelete.add(id);
                }
            }
        }
    }

    private static void filterRedundantIdFromDb(Map<String, Map<UUID, UUID>> secretIdsMap, Map<String, Set<UUID>> desired, Set<UUID> idsToDelete) {
        for (var entry : secretIdsMap.entrySet()) {
            String nk = entry.getKey();
            Map<UUID, UUID> haveList = entry.getValue();
            if (haveList == null || haveList.isEmpty()) continue;

            if (!desired.containsKey(nk)) {
                idsToDelete.addAll(
                        haveList.values().stream().toList()
                );
            }
        }
    }

    /**
     * Unified method to sync all types of profiles: metadata-driven and node-config-driven
     */
    private void syncAllProfiles(UUID workflowId, WorkflowDefinition definition) {
        // Use the metadata that WorkflowContextService already populated from scanning config.profile
        ProfileResolutionResult allProfiles = resolveProfilesFromMetadata(workflowId, definition);

        if (!allProfiles.missingAssignments().isEmpty()) {
            markWorkflowProfilesMissing(workflowId, allProfiles.missingAssignments());
        }

        Map<String, UUID> desiredProfileByNode = allProfiles.desiredProfiles();
        if (desiredProfileByNode.isEmpty()) {
            // No profiles needed, remove all existing links
            List<SecretProfileNodeLinkInfo> existingLinks = secretLinkService.getProfileLinksByWorkflowId(workflowId);
            if (!existingLinks.isEmpty()) {
                secretLinkService.unlinkProfilesByWorkflowId(workflowId);
            }
            return;
        }

        // Apply profile links
        applyProfileLinks(workflowId, desiredProfileByNode);
    }

    /**
     * Resolve profiles from metadata - which already contains both metadata.profiles
     * and profiles scanned from config.profile sections by WorkflowContextService
     */
    private ProfileResolutionResult resolveProfilesFromMetadata(UUID workflowId, WorkflowDefinition definition) {
        Map<String, WorkflowProfileBinding> assignments = definition.metadata().profileAssignments();
        if (assignments == null || assignments.isEmpty()) {
            return new ProfileResolutionResult(Map.of(), List.of());
        }

        Map<String, UUID> desiredProfileByNode = new HashMap<>();
        List<MissingProfileAssignment> missingAssignments = new ArrayList<>();
        Map<String, BaseWorkflowNode> nodeMap = definition.nodes().asMap();
        Map<String, Map<String, UUID>> profilesByPlugin = profileSecretService.getPluginProfileMap(workflowId);

        assignments.forEach((nodeKey, binding) -> {
            if (binding == null) {
                return;
            }

            BaseWorkflowNode node = nodeMap.get(nodeKey);
            if (node == null || node.getPluginNode() == null) {
                missingAssignments.add(new MissingProfileAssignment(nodeKey, binding.profileKey(), null, "metadata"));
                return;
            }

            String pluginKey = binding.pluginKey() != null ? binding.pluginKey() : node.getPluginNode().getPluginKey();
            String profileKey = binding.profileKey();
            if (pluginKey == null || pluginKey.isBlank() || profileKey == null || profileKey.isBlank()) {
                missingAssignments.add(new MissingProfileAssignment(nodeKey, profileKey, pluginKey, "metadata"));
                return;
            }

            UUID profileId = binding.profileId();
            String candidateName = binding.profileName() != null ? binding.profileName() : profileKey;

            if (profileId == null) {
                Map<String, UUID> pluginProfiles = profilesByPlugin.getOrDefault(pluginKey, Map.of());
                profileId = pluginProfiles.get(candidateName);
                if (profileId == null) {
                    String namespaced = pluginKey + "." + profileKey;
                    profileId = pluginProfiles.get(namespaced);
                    if (profileId != null) {
                        candidateName = namespaced;
                    }
                }
                if (profileId == null) {
                    profileId = profileSecretService.resolveProfileId(workflowId, pluginKey, candidateName);
                }
            }

            if (profileId != null) {
                desiredProfileByNode.put(nodeKey, profileId);
            } else {
                missingAssignments.add(new MissingProfileAssignment(nodeKey, profileKey, pluginKey, "resolved"));
            }
        });
        return new ProfileResolutionResult(desiredProfileByNode, missingAssignments);
    }

    /**
     * Apply profile links - unified linking logic
     */
    private void applyProfileLinks(UUID workflowId, Map<String, UUID> desiredProfileByNode) {
        List<SecretProfileNodeLinkInfo> existingLinks = secretLinkService.getProfileLinksByWorkflowId(workflowId);

        Map<String, Map<UUID, UUID>> existingByNode = existingLinks.stream()
                .collect(Collectors.groupingBy(
                        SecretProfileNodeLinkInfo::getNodeKey,
                        Collectors.collectingAndThen(
                                Collectors.toList(),
                                list -> list.stream()
                                        .collect(Collectors.toMap(
                                                SecretProfileNodeLinkInfo::getProfileId,
                                                SecretProfileNodeLinkInfo::getId,
                                                (current, duplicate) -> duplicate
                                        ))
                        )
                ));

        List<LinkProfileToNodeRequest> linksToInsert = new ArrayList<>();
        Set<UUID> linkIdsToDelete = new HashSet<>();

        processToExtractProfileLinkRequest(desiredProfileByNode, existingByNode, linksToInsert, linkIdsToDelete);

        if (!linkIdsToDelete.isEmpty()) {
            secretLinkService.unlinkProfileLinks(linkIdsToDelete);
            log.debug("Unlinked {} obsolete profile links", linkIdsToDelete.size());
        }

        if (!linksToInsert.isEmpty()) {
            secretLinkService.linkProfilesButchToNode(workflowId, linksToInsert);
            log.debug("Created {} new profile links", linksToInsert.size());
        }
    }

    private static void processToExtractProfileLinkRequest(Map<String, UUID> desiredProfileByNode,
                                                           Map<String, Map<UUID, UUID>> existingByNode,
                                                           List<LinkProfileToNodeRequest> linksToInsert,
                                                           Set<UUID> linkIdsToDelete) {
        desiredProfileByNode.forEach((nodeKey, desiredProfileId) -> {
            Map<UUID, UUID> existing = existingByNode.get(nodeKey);
            if (existing == null || existing.isEmpty()) {
                linksToInsert.add(new LinkProfileToNodeRequest(desiredProfileId, nodeKey));
                return;
            }

            if (!existing.containsKey(desiredProfileId)) {
                linksToInsert.add(new LinkProfileToNodeRequest(desiredProfileId, nodeKey));
            }

            existing.forEach((profileId, linkId) -> {
                if (!profileId.equals(desiredProfileId)) {
                    linkIdsToDelete.add(linkId);
                }
            });
        });

        existingByNode.forEach((nodeKey, linksByProfile) -> {
            if (!desiredProfileByNode.containsKey(nodeKey)) {
                linkIdsToDelete.addAll(linksByProfile.values());
            }
        });
    }

    private record ProfileResolutionResult(Map<String, UUID> desiredProfiles, List<MissingProfileAssignment> missingAssignments) { }

    private record MissingProfileAssignment(String nodeKey, String profileKey, String pluginKey, String source) {

    }

    private void markWorkflowProfilesMissing(UUID workflowId, List<MissingProfileAssignment> missingAssignments) {
        if (missingAssignments.isEmpty()) {
            return;
        }
        workflowRepository.findById(workflowId).ifPresent(workflow -> {
            workflow.setIsActive(false);
            List<ValidationError> errors = new ArrayList<>();
            ValidationResult current = workflow.getLastValidation();
            if (current != null && current.getErrors() != null) {
                errors.addAll(current.getErrors());
            }
            for (MissingProfileAssignment assignment : missingAssignments) {
                String message = "Profile key '" + assignment.profileKey() + "' could not be resolved for node '" + assignment.nodeKey() + "'"
                        + (assignment.pluginKey() != null ? " (plugin '" + assignment.pluginKey() + "')" : "")
                        + " from " + assignment.source() + " source";
                errors.add(ValidationError.builder()
                        .nodeKey(assignment.nodeKey())
                        .errorType("definition")
                        .errorCode(ValidationErrorCode.VALIDATION_ERROR)
                        .path(assignment.source().equals("metadata") ?
                              "metadata.profileAssignments." + assignment.nodeKey() :
                              "config.profileKeys")
                        .message(message)
                        .build());
            }
            workflow.setLastValidation(new ValidationResult("unified-profile-sync", errors));
            workflow.setLastValidationAt(OffsetDateTime.now());
            workflow.setLastValidationPublishAttempt(false);
            workflowRepository.save(workflow);
        });
    }
}
