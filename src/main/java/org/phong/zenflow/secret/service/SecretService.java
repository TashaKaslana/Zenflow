package org.phong.zenflow.secret.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.phong.zenflow.core.services.AuthService;
import org.phong.zenflow.log.auditlog.annotations.AuditLog;
import org.phong.zenflow.log.auditlog.enums.AuditAction;
import org.phong.zenflow.project.service.ProjectService;
import org.phong.zenflow.secret.dto.CreateSecretBatchRequest;
import org.phong.zenflow.secret.dto.CreateSecretRequest;
import org.phong.zenflow.secret.dto.SecretDto;
import org.phong.zenflow.secret.dto.UpdateSecretRequest;
import org.phong.zenflow.secret.exception.SecretDomainException;
import org.phong.zenflow.secret.exception.SecretNotFoundException;
import org.phong.zenflow.secret.infrastructure.mapstruct.SecretMapper;
import org.phong.zenflow.secret.infrastructure.persistence.entity.Secret;
import org.phong.zenflow.secret.infrastructure.persistence.repository.SecretRepository;
import org.phong.zenflow.secret.util.AESUtil;
import org.phong.zenflow.user.service.UserService;
import org.phong.zenflow.workflow.infrastructure.persistence.repository.WorkflowRepository;
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
    private final SecretMapper secretMapper;
    private final AESUtil aesUtil;
    private final UserService userService;
    private final WorkflowRepository workflowRepository;
    private final ProjectService projectService;
    private final AuthService authService;

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
        log.info("Creating new secret with key: {}", request.key());
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
        secret.setUser(userService.getReferenceById(authService.getUserIdFromContext()));
        secret.setProject(req.projectId() != null ? projectService.getReferenceById(req.projectId()) : null);
        secret.setWorkflow(req.workflowId() != null ? workflowRepository.getReferenceById(req.workflowId()) : null);
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

    @Transactional(readOnly = true)
    public Map<String, List<UUID>> getSecretIdsByKey(UUID workflowId) {
        return secretRepository.findByWorkflowId(workflowId)
                .stream()
                .collect(Collectors.groupingBy(
                        Secret::getKey,
                        Collectors.mapping(Secret::getId, Collectors.toList())
                ));
    }

    @Transactional(readOnly = true)
    public List<UUID> resolveSecretIds(UUID workflowId, String key) {
        Map<String, List<UUID>> map = getSecretIdsByKey(workflowId);
        return map.getOrDefault(key, List.of());
    }

    @Transactional(readOnly = true)
    public Map<String, String> getSecretsKeyMapByWorkflowId(UUID workflowId) {
        return secretRepository.findByWorkflowId(workflowId)
                .stream()
                .collect(Collectors.toMap(
                        Secret::getKey,
                        s -> {
                            try {
                                return aesUtil.decrypt(s.getEncryptedValue());
                            } catch (Exception e) {
                                throw new SecretDomainException("Can't decrypt value for workflowId: " + workflowId, e);
                            }
                        },
                        (existing, replacement) -> replacement
                ));
    }
}
