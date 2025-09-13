package org.phong.zenflow.secret.controller;

import lombok.AllArgsConstructor;
import org.phong.zenflow.core.responses.RestApiResponse;
import org.phong.zenflow.secret.dto.CreateProfileSecretsRequest;
import org.phong.zenflow.secret.dto.CreateSecretBatchRequest;
import org.phong.zenflow.secret.dto.CreateSecretRequest;
import org.phong.zenflow.secret.dto.ProfileSecretListDto;
import org.phong.zenflow.secret.dto.LinkProfileToNodeRequest;
import org.phong.zenflow.secret.dto.LinkSecretToNodeRequest;
import org.phong.zenflow.secret.dto.SecretDto;
import org.phong.zenflow.secret.dto.UpdateSecretRequest;
import org.phong.zenflow.secret.service.SecretService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
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
    public ResponseEntity<RestApiResponse<ProfileSecretListDto>> getProfileSecretsByWorkflowId(@PathVariable UUID workflowId) {
        var secrets = secretService.getProfileSecretMapByWorkflowId(workflowId);
        return RestApiResponse.success(secrets);
    }

    @GetMapping("/workflows/{workflowId}/map/secrets-by-key")
    public ResponseEntity<RestApiResponse<Map<String, String>>> getSecretsKeyMap(@PathVariable UUID workflowId) {
        Map<String, String> map = secretService.getSecretsKeyMapByWorkflowId(workflowId);
        return RestApiResponse.success(map);
    }

    @GetMapping("/workflows/{workflowId}/map/profiles-by-name")
    public ResponseEntity<RestApiResponse<Map<String, Map<String, String>>>> getProfilesKeyMap(@PathVariable UUID workflowId) {
        Map<String, Map<String, String>> map = secretService.getProfilesKeyMapByWorkflowId(workflowId);
        return RestApiResponse.success(map);
    }

    @PostMapping("/workflows/{workflowId}/profile")
    public ResponseEntity<RestApiResponse<ProfileSecretListDto>> createProfileSecretsForWorkflow(@PathVariable UUID workflowId, @RequestBody CreateProfileSecretsRequest secrets) {
        var profileSecrets = secretService.createProfileSecrets(workflowId, secrets);
        return RestApiResponse.created(profileSecrets);
    }

    @PostMapping("/workflows/{workflowId}/profile/link")
    public ResponseEntity<RestApiResponse<Void>> linkProfileToNode(@PathVariable UUID workflowId, @RequestBody LinkProfileToNodeRequest request) {
        secretService.linkProfileToNode(workflowId, request);
        return RestApiResponse.noContent();
    }
    @GetMapping("/workflows/{workflowId}/profile/link/{nodeKey}")
    public ResponseEntity<RestApiResponse<org.phong.zenflow.secret.dto.NodeProfileLinkDto>> getProfileLink(@PathVariable UUID workflowId, @PathVariable String nodeKey) {
        var dto = secretService.getProfileLink(workflowId, nodeKey);
        return RestApiResponse.success(dto);
    }
    @DeleteMapping("/workflows/{workflowId}/profile/link/{nodeKey}")
    public ResponseEntity<RestApiResponse<Void>> unlinkProfileFromNode(@PathVariable UUID workflowId, @PathVariable String nodeKey) {
        secretService.unlinkProfileFromNode(workflowId, nodeKey);
        return RestApiResponse.noContent();
    }

    @PostMapping("/workflows/{workflowId}/secret/link")
    public ResponseEntity<RestApiResponse<Void>> linkSecretToNode(@PathVariable UUID workflowId, @RequestBody LinkSecretToNodeRequest request) {
        secretService.linkSecretToNode(workflowId, request);
        return RestApiResponse.noContent();
    }
    @GetMapping("/workflows/{workflowId}/secret/link/{nodeKey}")
    public ResponseEntity<RestApiResponse<org.phong.zenflow.secret.dto.NodeSecretLinksDto>> getSecretLinks(@PathVariable UUID workflowId, @PathVariable String nodeKey) {
        var dto = secretService.getSecretLinks(workflowId, nodeKey);
        return RestApiResponse.success(dto);
    }
    @DeleteMapping("/workflows/{workflowId}/secret/link/{nodeKey}/{secretId}")
    public ResponseEntity<RestApiResponse<Void>> unlinkSecretFromNode(@PathVariable UUID workflowId, @PathVariable String nodeKey, @PathVariable UUID secretId) {
        secretService.unlinkSecretFromNode(workflowId, nodeKey, secretId);
        return RestApiResponse.noContent();
    }
    @DeleteMapping("/workflows/{workflowId}/secret/link/{nodeKey}")
    public ResponseEntity<RestApiResponse<Void>> unlinkAllSecretsFromNode(@PathVariable UUID workflowId, @PathVariable String nodeKey) {
        secretService.unlinkAllSecretsFromNode(workflowId, nodeKey);
        return RestApiResponse.noContent();
    }
}
