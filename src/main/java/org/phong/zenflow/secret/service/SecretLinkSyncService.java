package org.phong.zenflow.secret.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.secret.dto.LinkProfileToNodeRequest;
import org.phong.zenflow.secret.dto.LinkSecretToNodeRequest;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.WorkflowDefinition;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SecretLinkSyncService {
    private final SecretService secretService;

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

        for (var entry : desired.entrySet()) {
            String nk = entry.getKey();
            Set<UUID> want = entry.getValue();
            List<UUID> haveList = secretService.getLinkedSecretIds(workflowId, nk);
            Set<UUID> have = new HashSet<>(haveList);

            // Add missing
            for (UUID id : want) {
                if (!have.contains(id)) {
                    secretService.linkSecretToNode(workflowId, new LinkSecretToNodeRequest(id, nk));
                }
            }

            // Remove extras not declared
            for (UUID id : have) {
                if (!want.contains(id)) {
                    secretService.unlinkSecretFromNode(workflowId, nk, id);
                }
            }
        }
    }

    private void syncProfilesFromMetadata(UUID workflowId, WorkflowDefinition definition) {
        if (definition.metadata().profiles() == null) return;

        Map<String, UUID> desiredProfileByNode = getDesiredProfileIdsForNodes(workflowId, definition);

        definition.nodes().forEach((nk, node) -> {
            var existing = secretService.getProfileLink(workflowId, nk);
            UUID want = desiredProfileByNode.get(nk);
            if (want != null) {
                if (existing == null || !want.equals(existing.profileId())) {
                    secretService.linkProfileToNode(workflowId, new LinkProfileToNodeRequest(want, nk));
                }
            } else {
                if (existing != null) {
                    secretService.unlinkProfileFromNode(workflowId, nk);
                }
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
                UUID pid = secretService.resolveProfileId(workflowId, pluginKey, profileName);
                if (pid != null) {
                    desiredProfileByNode.put(nk, pid);
                }
            }
        });
        return desiredProfileByNode;
    }
}

