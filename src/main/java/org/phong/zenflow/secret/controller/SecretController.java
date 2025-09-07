package org.phong.zenflow.secret.controller;

import lombok.AllArgsConstructor;
import org.phong.zenflow.core.responses.RestApiResponse;
import org.phong.zenflow.secret.dto.CreateProfileSecretsRequest;
import org.phong.zenflow.secret.dto.CreateSecretBatchRequest;
import org.phong.zenflow.secret.dto.CreateSecretRequest;
import org.phong.zenflow.secret.dto.ProfileSecretDto;
import org.phong.zenflow.secret.dto.SecretDto;
import org.phong.zenflow.secret.dto.UpdateSecretRequest;
import org.phong.zenflow.secret.service.SecretService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/secrets")
@AllArgsConstructor
public class SecretController {
    private SecretService secretService;

    @PostMapping
    public ResponseEntity<RestApiResponse<SecretDto>> createSecret(@RequestBody CreateSecretRequest request) {
        SecretDto secretDto = secretService.createSecret(request);
        return RestApiResponse.created(secretDto);
    }

    @PostMapping("/batch")
    public ResponseEntity<RestApiResponse<List<SecretDto>>> createSecretsBatch(@RequestBody CreateSecretBatchRequest request) {
        List<SecretDto> secretsBatch = secretService.createSecretsBatch(request);
        return RestApiResponse.created(secretsBatch);
    }

    @GetMapping
    public ResponseEntity<RestApiResponse<List<SecretDto>>> getAllSecrets() {
        List<SecretDto> secrets = secretService.getAllSecrets();
        return RestApiResponse.success(secrets);
    }

    @GetMapping("/paginated")
    public ResponseEntity<RestApiResponse<List<SecretDto>>> getAllSecrets(Pageable pageable) {
        Page<SecretDto> secrets = secretService.getAllSecrets(pageable);
        return RestApiResponse.success(secrets);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RestApiResponse<SecretDto>> getSecretById(@PathVariable UUID id) {
        SecretDto secret = secretService.getSecretById(id);
        return RestApiResponse.success(secret);
    }

    @PutMapping("/{id}")
    public ResponseEntity<RestApiResponse<SecretDto>> updateSecret(@PathVariable UUID id, @RequestBody UpdateSecretRequest request) {
        SecretDto updatedSecret = secretService.updateSecret(id, request);
        return RestApiResponse.success(updatedSecret);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<RestApiResponse<Void>> deleteSecret(@PathVariable UUID id) {
        secretService.deleteSecret(id);
        return RestApiResponse.noContent();
    }

    @DeleteMapping("/{id}/force")
    public ResponseEntity<RestApiResponse<Void>> forceDeleteSecret(@PathVariable UUID id) {
        secretService.hardDeleteSecret(id);
        return RestApiResponse.noContent();
    }

    @PatchMapping("/{id}/restore")
    public ResponseEntity<RestApiResponse<Void>> restoreSecret(@PathVariable UUID id) {
        secretService.restoreSecret(id);
        return RestApiResponse.noContent();
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<RestApiResponse<List<SecretDto>>> getSecretsByUserId(@PathVariable UUID userId) {
        List<SecretDto> secrets = secretService.getSecretsByUserId(userId);
        return RestApiResponse.success(secrets);
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<RestApiResponse<List<SecretDto>>> getSecretsByProjectId(@PathVariable UUID projectId) {
        List<SecretDto> secrets = secretService.getSecretsByProjectId(projectId);
        return RestApiResponse.success(secrets);
    }

    @GetMapping("/workflow/{workflowId}")
    public ResponseEntity<RestApiResponse<List<SecretDto>>> getSecretsByWorkflowId(@PathVariable UUID workflowId) {
        List<SecretDto> secrets = secretService.getSecretsByWorkflowId(workflowId);
        return RestApiResponse.success(secrets);
    }

    @GetMapping("/workflows/{workflowId}/profile")
    public ResponseEntity<RestApiResponse<ProfileSecretDto>> getProfileSecretsByWorkflowId(@PathVariable UUID workflowId) {
        var secrets = secretService.getProfileSecretMapByWorkflowId(workflowId);
        return RestApiResponse.success(secrets);
    }

    @PostMapping("/workflows/{workflowId}/profile")
    public ResponseEntity<RestApiResponse<ProfileSecretDto>> createProfileSecretsForWorkflow(@PathVariable UUID workflowId, @RequestBody CreateProfileSecretsRequest secrets) {
        var profileSecrets = secretService.createProfileSecrets(workflowId, secrets);
        return RestApiResponse.created(profileSecrets);
    }
}