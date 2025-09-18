package org.phong.zenflow.workflow.subdomain.schema_validator.service;

import lombok.AllArgsConstructor;
import org.phong.zenflow.secret.service.SecretService;
import org.phong.zenflow.workflow.subdomain.evaluator.services.TemplateService;
import org.phong.zenflow.workflow.subdomain.node_definition.constraints.WorkflowConstraints;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.BaseWorkflowNode;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.WorkflowDefinition;
import org.phong.zenflow.workflow.subdomain.schema_validator.dto.ValidationError;
import org.phong.zenflow.workflow.subdomain.schema_validator.enums.ValidationErrorCode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@AllArgsConstructor
@Component
public class WorkflowExistenceValidation {
    private final SecretService secretService;
    private final TemplateService templateService;

    public List<ValidationError> validateExistence(WorkflowDefinition workflow) {
        List<ValidationError> errors = new ArrayList<>();
        errors.addAll(hasReservedKeys(workflow));
        errors.addAll(validateNodeReference(workflow));
        return errors;
    }

    private List<ValidationError> hasReservedKeys(WorkflowDefinition workflow) {
        List<ValidationError> errors = new ArrayList<>();
        if (workflow.metadata() == null || workflow.metadata().aliases() == null) {
            return errors;
        }

        String zenflowAlias = WorkflowConstraints.ZENFLOW_PREFIX.key();
        String secretsAlias = WorkflowConstraints.RESERVED_SECRETS_PREFIX.key();
        if (secretsAlias.endsWith(".")) {
            secretsAlias = secretsAlias.substring(0, secretsAlias.length() - 1);
        }
        String profilesAlias = WorkflowConstraints.RESERVED_PROFILES_PREFIX.key();
        if (profilesAlias.endsWith(".")) {
            profilesAlias = profilesAlias.substring(0, profilesAlias.length() - 1);
        }

        for (String aliasName : workflow.metadata().aliases().keySet()) {
            if (zenflowAlias.equalsIgnoreCase(aliasName)
                    || secretsAlias.equalsIgnoreCase(aliasName)
                    || profilesAlias.equalsIgnoreCase(aliasName)) {
                errors.add(ValidationError.builder()
                        .nodeKey("aliases") // Alias is not tied to a specific node
                        .errorType("definition")
                        .errorCode(ValidationErrorCode.RESERVED_KEY)
                        .path("metadata.aliases." + aliasName)
                        .message("Alias name '" + aliasName + "' is reserved and cannot be used.")
                        .value(aliasName)
                        .build());
            }
        }

        for (String nodeKey : workflow.nodes().keys()) {
            if (WorkflowConstraints.isReservedKey(nodeKey)) {
                errors.add(ValidationError.builder()
                        .nodeKey(nodeKey)
                        .errorType("definition")
                        .errorCode(ValidationErrorCode.RESERVED_KEY)
                        .path("nodes." + nodeKey)
                        .message("Node key '" + nodeKey + "' is reserved and cannot be used.")
                        .value(nodeKey)
                        .build());
            }
        }
        return errors;
    }

    public List<ValidationError> validateSecretAndProfileExistence(UUID workflowId, WorkflowDefinition workflow) {
        List<ValidationError> extra = new ArrayList<>();

        validateSecretExistence(workflowId, workflow, extra);
        validateProfileExistence(workflowId, workflow, extra);

        return extra;
    }

    private void validateProfileExistence(UUID workflowId, WorkflowDefinition workflow, List<ValidationError> extra) {
        if (workflow != null && workflow.nodes() != null && workflow.metadata() != null && workflow.metadata().profiles() != null) {
            Map<String, List<String>> profileUsage = workflow.metadata().profiles();
            Map<String, BaseWorkflowNode> nodeMap = workflow.nodes().asMap();

            profileUsage.forEach((profileName, nodeKeys) -> {
                if (nodeKeys == null) return;
                for (String nk : nodeKeys) {
                    BaseWorkflowNode node = nodeMap.get(nk);
                    if (node == null || node.getPluginNode() == null) {
                        continue; // node existence validated elsewhere
                    }

                    String pluginKey = node.getPluginNode().getPluginKey();
                    UUID profileId = secretService.resolveProfileId(workflowId, pluginKey, profileName);
                    if (profileId == null) {
                        extra.add(ValidationError.builder()
                                .nodeKey(nk)
                                .errorType("definition")
                                .errorCode(ValidationErrorCode.INVALID_REFERENCE)
                                .path("metadata.profiles." + profileName)
                                .message("Profile '" + profileName + "' does not exist for plugin '" + pluginKey + "'")
                                .value(profileName)
                                .expectedType("existing_profile_name")
                                .build());
                    }
                }
            });

            // Warn if a node appears under multiple profiles (duplicate assignment)
            Map<String, List<String>> profilesByNode = new java.util.HashMap<>();
            profileUsage.forEach((profileName, nodeKeys) -> {
                if (nodeKeys == null) return;
                for (String nk : nodeKeys) {
                    profilesByNode.computeIfAbsent(nk, k -> new ArrayList<>()).add(profileName);
                }
            });
            profilesByNode.forEach((nk, names) -> {
                if (names != null && names.size() > 1) {
                    extra.add(ValidationError.builder()
                            .nodeKey(nk)
                            .errorType("definition-warning")
                            .errorCode(ValidationErrorCode.INVALID_VALUE)
                            .path("metadata.profiles")
                            .message("Node '" + nk + "' is listed under multiple profiles: " + names)
                            .value(String.join(",", names))
                            .expectedType("single_profile_assignment")
                            .build());
                }
            });
        }
    }

    private void validateSecretExistence(UUID workflowId, WorkflowDefinition workflow, List<ValidationError> extra) {
        if (workflow != null && workflow.nodes() != null && workflow.metadata() != null && workflow.metadata().secrets() != null) {
            Map<String, List<String>> secretsUsage = workflow.metadata().secrets();

            secretsUsage.forEach((secretKey, nodeKeys) -> {
                // Resolve secret IDs by key
                List<UUID> ids = secretService.resolveSecretIds(workflowId, secretKey);
                if (ids == null || ids.isEmpty()) {
                    extra.add(ValidationError.builder()
                            .nodeKey("metadata")
                            .errorType("definition")
                            .errorCode(ValidationErrorCode.INVALID_REFERENCE)
                            .path("metadata.secrets." + secretKey)
                            .message("Secret key '" + secretKey + "' does not exist in this workflow")
                            .value(secretKey)
                            .expectedType("existing_secret_key")
                            .build());
                }
            });
        }
    }

    /**
     * Validate that templates referenced by each node's configuration resolve to existing nodes.
     * Only template references are checked here; allowed pseudo-references starting with
     * `zenflow.secrets.` or `zenflow.profiles.` are considered valid. Aliases from workflow
     * metadata are used to resolve references.
     *
     * @param definition The workflow definition containing nodes and metadata
     * @return A list of validation errors for missing node references
     */
    private List<ValidationError> validateNodeReference(WorkflowDefinition definition) {
        Set<String> nodeKeys = definition.nodes().keys();
        Map<String, String> aliases = definition.metadata() != null && definition.metadata().aliases() != null
                ? definition.metadata().aliases() : Map.of();

        List<ValidationError> errors = new ArrayList<>();
        for (BaseWorkflowNode node : definition.nodes().values()) {
            if (node.getConfig() == null) {
                continue;
            }

            Set<String> templates = templateService.extractRefs(node.getConfig());

            for (String template : templates) {
                String referencedNode = templateService.getReferencedNode(template, aliases);

                // Only validate existence - dependency direction is handled by WorkflowDependencyValidator
                if (referencedNode == null
                        || nodeKeys.contains(referencedNode)
                        || WorkflowConstraints.isReservedKey(referencedNode)) {
                    continue;
                }

                errors.add(ValidationError.builder()
                        .nodeKey(node.getKey())
                        .errorType("definition")
                        .errorCode(ValidationErrorCode.MISSING_NODE_REFERENCE)
                        .path(node.getKey() + ".config")
                        .message("Referenced node '" + referencedNode + "' does not exist in workflow")
                        .template("{{" + template + "}}")
                        .value(referencedNode)
                        .expectedType("existing_node_key")
                        .schemaPath("$.nodes[?(@.key=='" + node.getKey() + "')].config")
                        .build());
            }
        }

        return errors;
    }
}
