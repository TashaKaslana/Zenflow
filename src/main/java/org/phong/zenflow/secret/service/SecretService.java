package org.phong.zenflow.secret.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.phong.zenflow.core.services.AuthService;
import org.phong.zenflow.log.auditlog.annotations.AuditLog;
import org.phong.zenflow.log.auditlog.enums.AuditAction;
import org.phong.zenflow.project.service.ProjectService;
import org.phong.zenflow.secret.dto.CreateProfileSecretsRequest;
import org.phong.zenflow.secret.dto.CreateSecretBatchRequest;
import org.phong.zenflow.secret.dto.CreateSecretRequest;
import org.phong.zenflow.secret.dto.ProfileSecretListDto;
import org.phong.zenflow.secret.dto.SecretDto;
import org.phong.zenflow.secret.dto.UpdateSecretRequest;
import org.phong.zenflow.secret.enums.SecretScope;
import org.phong.zenflow.secret.exception.SecretDomainException;
import org.phong.zenflow.secret.exception.SecretNotFoundException;
import org.phong.zenflow.secret.infrastructure.mapstruct.SecretMapper;
import org.phong.zenflow.secret.infrastructure.persistence.entity.Secret;
import org.phong.zenflow.secret.infrastructure.persistence.entity.SecretProfile;
import org.phong.zenflow.secret.infrastructure.persistence.repository.SecretProfileRepository;
import org.phong.zenflow.secret.infrastructure.persistence.repository.SecretRepository;
import org.phong.zenflow.secret.util.AESUtil;
import org.phong.zenflow.user.service.UserService;
import org.phong.zenflow.workflow.service.WorkflowService;
import org.phong.zenflow.plugin.subdomain.node.service.PluginNodeService;
import org.phong.zenflow.plugin.services.PluginService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@Slf4j
@Transactional
public class SecretService {
    private final SecretRepository secretRepository;
    private final SecretProfileRepository secretProfileRepository;
    private final SecretMapper secretMapper;
    private final AESUtil aesUtil;
    private final UserService userService;
    private final WorkflowService workflowService;
    private final ProjectService projectService;
    private final AuthService authService;
    private final SecretProfileSchemaValidator validator;
    private final PluginNodeService pluginNodeService;
    private final PluginService pluginService;

    @Transactional(readOnly = true)
    public List<SecretDto> getAllSecrets() {
        return secretRepository.findAll()
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<SecretDto> getAllSecrets(Pageable pageable) {
        return secretRepository.findAll(pageable)
                .map(this::mapToDto);
    }

    @Transactional(readOnly = true)
    public SecretDto getSecretById(UUID id) {
        return secretRepository.findById(id)
                .map(this::mapToDto).orElseThrow(
                        () -> {
                            log.error("Secret not found with id on get: {}", id);
                            return new SecretNotFoundException("Secret not found with id: " + id);
                        }
                );
    }


    @AuditLog(
            action = AuditAction.SECRET_CREATE,
            targetIdExpression = "#result.id"
    )
    public SecretDto createSecret(CreateSecretRequest request) {
        log.info("Creating new secret with profileId: {} and key: {}", request.profileId(), request.key());
        try {
            Secret secret = getSecretFromRequest(request);

            Secret savedSecret = secretRepository.save(secret);
            return mapToDto(savedSecret);
        } catch (Exception e) {
            log.error("Failed to create secret: {}", e.getMessage(), e);
            throw new SecretDomainException("Failed to create secret", e);
        }
    }

    @AuditLog(
            action = AuditAction.SECRET_CREATE,
            targetIdExpression = "#result.id",
            description = "Create batch of secrets"
    )
    public List<SecretDto> createSecretsBatch(CreateSecretBatchRequest request) {
        log.info("Creating batch of secrets, count: {}", request.secrets().size());
        try {
            List<Secret> secrets = request.secrets().stream()
                    .map(this::getSecretFromRequest)
                    .collect(Collectors.toList());

            List<Secret> savedSecrets = secretRepository.saveAll(secrets);
            return savedSecrets.stream().map(this::mapToDto).collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to create secrets batch: {}", e.getMessage(), e);
            throw new SecretDomainException("Failed to create secrets batch", e);
        }
    }

    @NotNull
    private Secret getSecretFromRequest(CreateSecretRequest req) {
        Secret secret = secretMapper.toEntity(req);
        secret.setUser(userService.getReferenceById(req.userId()));
        secret.setProject(req.projectId() != null ? projectService.getReferenceById(req.projectId()) : null);
        secret.setWorkflow(req.workflowId() != null ? workflowService.getReferenceById(req.workflowId()) : null);
        secret.setProfile(secretProfileRepository.getReferenceById(req.profileId()));
        try {
            secret.setEncryptedValue(aesUtil.encrypt(req.value()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return secret;
    }

    @AuditLog(
            action = AuditAction.SECRET_UPDATE,
            targetIdExpression = "#id"
    )
    public SecretDto updateSecret(UUID id, UpdateSecretRequest request) {
        return secretRepository.findById(id)
                .map(secret -> {
                    try {
                        secretMapper.updateEntityFromDto(request, secret);

                        if (request.profileId() != null) {
                            SecretProfile profile = secretProfileRepository.getReferenceById(request.profileId());
                            secret.setProfile(profile);
                        }

                        secret.setEncryptedValue(aesUtil.encrypt(request.value()));

                        Secret updatedSecret = secretRepository.save(secret);
                        return mapToDto(updatedSecret);
                    } catch (Exception e) {
                        log.error("Failed to update secret with id {}: {}", id, e.getMessage(), e);
                        throw new SecretDomainException("Failed to update secret", e);
                    }
                })
                .orElseThrow(() -> {
                    log.error("Secret not found with id: {}", id);
                    return new SecretNotFoundException("Secret not found with id: " + id);
                });
    }

    @AuditLog(
            action = AuditAction.SECRET_DELETE,
            targetIdExpression = "#id"
    )
    public void deleteSecret(UUID id) {
        Secret secret = secretRepository.findById(id).orElseThrow(() -> {
            log.error("Secret not found with id on delete: {}", id);
            return new SecretNotFoundException("Secret not found with id: " + id);
        });

        secret.setDeletedAt(OffsetDateTime.now());

        secretRepository.save(secret);
    }

    @AuditLog(
            action = AuditAction.SECRET_RESTORE,
            targetIdExpression = "#id"
    )
    public void restoreSecret(UUID id) {
        Secret secret = secretRepository.findById(id).orElseThrow(() -> {
            log.error("Secret not found with id on restore: {}", id);
            return new SecretNotFoundException("Secret not found with id: " + id);
        });

        if (secret.getDeletedAt() == null) {
            log.warn("Secret with id {} is not deleted, cannot restore", id);
            return;
        }

        secret.setDeletedAt(null);
        secretRepository.save(secret);
    }

    @AuditLog(
            action = AuditAction.SECRET_DELETE,
            description = "Hard delete secret",
            targetIdExpression = "#id"
    )
    public void hardDeleteSecret(UUID id) {
        if (!secretRepository.existsById(id)) {
            log.error("Secret not found with id on hard delete: {}", id);
            throw new SecretNotFoundException("Secret not found with id: " + id);
        }
        secretRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<SecretDto> getSecretsByUserId(UUID userId) {
        return secretRepository.findByUserId(userId)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SecretDto> getSecretsByProjectId(UUID projectId) {
        return secretRepository.findByProjectId(projectId)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SecretDto> getSecretsByWorkflowId(UUID workflowId) {
        return secretRepository.findByWorkflowId(workflowId)
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    public Map<String, String> getSecretMapByWorkflowId(UUID workflowId) {
        return secretRepository.findByWorkflowId(workflowId)
                .stream()
                .collect(Collectors.toMap(
                        secret -> secret.getProfile().getName() + "." + secret.getKey(),
                        secret -> {
                            try {
                                return aesUtil.decrypt(secret.getEncryptedValue());
                            } catch (Exception e) {
                                throw new SecretDomainException("Can't encrypted value for workflowId: " + workflowId, e);
                            }
                        }
                ));
    }

    /**
     * Retrieves all secrets for a workflow grouped by their profile name.
     *
     * @param workflowId the workflow identifier
     * @return map keyed by profile name, each containing a map of secret key to decrypted value
     */
    public ProfileSecretListDto getProfileSecretMapByWorkflowId(UUID workflowId) {
        Map<String, Map<String, String>> collect = secretRepository.findByWorkflowId(workflowId)
                .stream()
                .collect(Collectors.groupingBy(
                        secret -> secret.getProfile().getName(),
                        Collectors.toMap(
                                Secret::getKey,
                                secret -> {
                                    try {
                                        return aesUtil.decrypt(secret.getEncryptedValue());
                                    } catch (Exception e) {
                                        throw new SecretDomainException("Can't encrypted value for workflowId: " + workflowId, e);
                                    }
                                },
                                (existing, replacement) -> replacement
                        )
                ));

        return new ProfileSecretListDto(collect);
    }

    public ProfileSecretListDto createProfileSecrets(UUID workflowId, CreateProfileSecretsRequest request) {
        if (request.isDuplicated()) {
            throw new SecretDomainException("Duplicate keys found in the request.");
        }

        boolean exists = secretProfileRepository.existsByNameAndWorkflowIdAndPluginId(
                request.getName(), workflowId, request.getPluginId());
        if (exists) {
            throw new SecretDomainException("Profile with name '" + request.getName() + "' already exists for this workflow.");
        }

        SecretProfile profile = new SecretProfile();
        profile.setWorkflow(workflowService.getReferenceById(workflowId));
        profile.setName(request.getName());
        profile.setScope(SecretScope.WORKFLOW);
        profile.setUser(authService.getReferenceCurrentUser());
        profile.setPlugin(pluginService.findPluginById(request.getPluginId()));
        if (request.getPluginNodeId() != null) {
            profile.setPluginNode(pluginNodeService.findById(request.getPluginNodeId()));
        }
        profile = secretProfileRepository.save(profile);

        SecretProfile finalProfile = profile;
        List<Secret> secrets = request.getSecrets().stream()
                .map(entry -> {
                    Secret secret = new Secret();
                    secret.setWorkflow(finalProfile.getWorkflow());
                    secret.setProfile(finalProfile);
                    secret.setKey(entry.getKey());
                    secret.setScope(SecretScope.WORKFLOW);

                    try {
                        secret.setEncryptedValue(aesUtil.encrypt(entry.getValue()));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    secret.setDescription(entry.getDescription());
                    secret.setTags(entry.getTags());
                    secret.setUser(authService.getReferenceCurrentUser());
                    return secret;
                })
                .collect(Collectors.toList());

        boolean isValid = validator.validate(request.getPluginId(), request.getPluginNodeId(), request.getSecrets());
        if (!isValid) {
            throw new SecretDomainException("Secrets do not conform to the required schema!");
        }

        List<Secret> savedSecrets = secretRepository.saveAll(secrets);

        Map<String, Map<String, String>> profileMap = savedSecrets.stream()
                .collect(Collectors.groupingBy(
                        s -> s.getProfile().getName(),
                        Collectors.toMap(
                                Secret::getKey,
                                secret -> {
                                    try {
                                        return aesUtil.decrypt(secret.getEncryptedValue());
                                    } catch (Exception e) {
                                        throw new SecretDomainException("Can't encrypted value for workflowId: " + workflowId, e);
                                    }
                                },
                                (existing, replacement) -> replacement
                        )
                ));

        return new ProfileSecretListDto(profileMap);
    }

    private SecretDto mapToDto(Secret secret) {
        try {
            SecretDto dto = secretMapper.toDto(secret);
            String decryptedValue = aesUtil.decrypt(secret.getEncryptedValue());
            dto.setValue(decryptedValue);

            return dto;
        } catch (Exception e) {
            log.error("Failed to decrypt secret value for id {}: {}", secret.getId(), e.getMessage(), e);
            throw new SecretDomainException("Failed to decrypt secret value", e);
        }
    }
}
