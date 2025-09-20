package org.phong.zenflow.secret.subdomain.link.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.secret.service.SecretService;
import org.phong.zenflow.secret.subdomain.link.dto.LinkProfileToNodeRequest;
import org.phong.zenflow.secret.subdomain.link.dto.SecretNodeLinkInsertRequestDto;
import org.phong.zenflow.secret.subdomain.link.infrastructure.projection.SecretNodeLinkInfo;
import org.phong.zenflow.secret.subdomain.link.infrastructure.projection.SecretProfileNodeLinkInfo;
import org.phong.zenflow.secret.subdomain.profile.service.ProfileSecretService;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.WorkflowDefinition;
import org.springframework.stereotype.Service;

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

    public void syncLinksFromMetadata(UUID workflowId, WorkflowDefinition definition) {
        if (definition == null || definition.metadata() == null) return;
        try {
            syncSecretsFromMetadata(workflowId, definition);
        } catch (Exception e) {
            log.warn("Secret auto-linking from metadata.secrets failed during upsert: {}", e.getMessage());
        }

        try {
            syncProfilesFromMetadata(workflowId, definition);
        } catch (Exception e) {
            log.warn("Profile auto-linking from metadata.profiles failed during upsert: {}", e.getMessage());
        }
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

    private void syncProfilesFromMetadata(UUID workflowId, WorkflowDefinition definition) {
        if (definition.metadata().profiles() == null) return;

        Map<String, UUID> desiredProfileByNode = getDesiredProfileIdsForNodes(workflowId, definition);
        List<SecretProfileNodeLinkInfo> existingLinks = secretLinkService.getProfileLinksByWorkflowId(workflowId);

        if (desiredProfileByNode.isEmpty()) {
            if (!existingLinks.isEmpty()) {
                secretLinkService.unlinkProfilesByWorkflowId(workflowId);
            }
            return;
        }

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
        }

        if (!linksToInsert.isEmpty()) {
            secretLinkService.linkProfilesButchToNode(workflowId, linksToInsert);
        }
    }

    private void processToExtractProfileLinkRequest(Map<String, UUID> desiredProfileByNode,
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

    private Map<String, UUID> getDesiredProfileIdsForNodes(UUID workflowId, WorkflowDefinition definition) {
        Map<String, List<String>> profiles = definition.metadata().profiles();

        // nodeKey -> desired profileId
        Map<String, UUID> desiredProfileByNode = new HashMap<>();
        profiles.forEach((profileName, nodes) -> {
            if (nodes == null) return;
            for (String nk : nodes) {
                var node = definition.nodes().get(nk);
                if (node == null || node.getPluginNode() == null) continue;
                String pluginKey = node.getPluginNode().getPluginKey();
                UUID pid = profileSecretService.resolveProfileId(workflowId, pluginKey, profileName);
                if (pid != null) {
                    desiredProfileByNode.put(nk, pid);
                }
            }
        });
        return desiredProfileByNode;
    }
}

