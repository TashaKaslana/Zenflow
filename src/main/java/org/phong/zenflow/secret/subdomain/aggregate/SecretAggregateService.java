package org.phong.zenflow.secret.subdomain.aggregate;

import lombok.AllArgsConstructor;
import org.phong.zenflow.secret.dto.AggregatedSecretSetupDto;
import org.phong.zenflow.secret.exception.SecretDomainException;
import org.phong.zenflow.secret.infrastructure.persistence.entity.Secret;
import org.phong.zenflow.secret.subdomain.profile.entity.SecretProfile;
import org.phong.zenflow.secret.infrastructure.persistence.repository.SecretProfileRepository;
import org.phong.zenflow.secret.infrastructure.persistence.repository.SecretRepository;
import org.phong.zenflow.secret.subdomain.link.infrastructure.entity.SecretNodeLink;
import org.phong.zenflow.secret.subdomain.link.infrastructure.entity.SecretProfileNodeLink;
import org.phong.zenflow.secret.subdomain.link.infrastructure.repository.ProfileSecretLinkRepository;
import org.phong.zenflow.secret.subdomain.link.infrastructure.repository.SecretNodeLinkRepository;
import org.phong.zenflow.secret.subdomain.link.infrastructure.repository.SecretProfileNodeLinkRepository;
import org.phong.zenflow.secret.util.AESUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class SecretAggregateService {
    private final SecretRepository secretRepository;
    private final ProfileSecretLinkRepository profileSecretLinkRepository;
    private final SecretProfileNodeLinkRepository secretProfileNodeLinkRepository;
    private final SecretNodeLinkRepository secretNodeLinkRepository;
    private final SecretProfileRepository secretProfileRepository;
    private final AESUtil aesUtil;

    @Transactional(readOnly = true)
    public AggregatedSecretSetupDto getAggregatedSecretsProfilesAndNodeIndex(UUID workflowId) {
        // Secrets keyed by secretId to avoid collisions on duplicate keys
        Map<String, String> secretsById = secretRepository.findByWorkflowId(workflowId)
                .stream()
                .collect(Collectors.toMap(
                        s -> s.getId().toString(),
                        s -> {
                            try {
                                return aesUtil.decrypt(s.getEncryptedValue());
                            } catch (Exception e) {
                                throw new SecretDomainException("Can't decrypt value for workflowId: " + workflowId, e);
                            }
                        },
                        (existing, replacement) -> replacement
                ));

        // Secret id -> key for reconstructing per-node key maps
        Map<String, String> secretKeys = secretRepository.findByWorkflowId(workflowId)
                .stream()
                .collect(Collectors.toMap(s -> s.getId().toString(), Secret::getKey, (a, b) -> b));

        // Profiles keyed by profileId, values map secretKey -> decrypted value for that profile
        Map<String, Map<String, String>> profilesById = profileSecretLinkRepository.findByWorkflowId(workflowId)
                .stream()
                .collect(Collectors.groupingBy(
                        link -> link.getProfile().getId().toString(),
                        Collectors.toMap(
                                link -> link.getSecret().getKey(),
                                link -> {
                                    try {
                                        return aesUtil.decrypt(link.getSecret().getEncryptedValue());
                                    } catch (Exception e) {
                                        throw new SecretDomainException("Can't decrypt value for workflowId: " + workflowId, e);
                                    }
                                },
                                (existing, replacement) -> replacement
                        )
                ));

        // Map nodeKey -> profileId (stable)
        Map<String, String> nodeProfiles = secretProfileNodeLinkRepository.findAllByWorkflowId(workflowId)
                .stream()
                .collect(Collectors.toMap(
                        SecretProfileNodeLink::getNodeKey,
                        link -> link.getProfile().getId().toString(),
                        (a, b) -> b
                ));

        // Map nodeKey -> [secretId] to reference stable identifiers
        Map<String, List<String>> nodeSecrets = secretNodeLinkRepository.findByWorkflowId(workflowId)
                .stream()
                .collect(Collectors.groupingBy(
                        SecretNodeLink::getNodeKey,
                        Collectors.mapping(
                                link -> link.getSecret().getId().toString(),
                                Collectors.toList()
                        )
                ));

        // Also expose profileId -> profileName for display purposes
        Map<String, String> profileNames = secretProfileRepository.findAll().stream()
                .filter(p -> p.getWorkflow() != null && workflowId.equals(p.getWorkflow().getId()))
                .collect(Collectors.toMap(p -> p.getId().toString(), SecretProfile::getName, (a, b) -> b));

        return new AggregatedSecretSetupDto(secretsById, profilesById, nodeProfiles, nodeSecrets, profileNames, secretKeys);
    }
}
