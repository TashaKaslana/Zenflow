package org.phong.zenflow.workflow.subdomain.schema_validator.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.core.utils.ObjectConversion;
import org.phong.zenflow.plugin.subdomain.execution.registry.PluginNodeExecutorRegistry;
import org.phong.zenflow.plugin.subdomain.execution.utils.TemplateEngine;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.BaseWorkflowNode;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.WorkflowDefinition;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto.OutputUsage;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto.WorkflowConfig;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.plugin.PluginNodeIdentifier;
import org.phong.zenflow.workflow.subdomain.schema_validator.dto.ValidationError;
import org.phong.zenflow.workflow.subdomain.schema_validator.dto.ValidationResult;
import org.phong.zenflow.workflow.subdomain.schema_validator.enums.ValidationErrorCode;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Service responsible for validating workflow definitions and their runtime configurations.
 * Performs two main phases of validation:<br>
 * 1. Definition-time validation - ensures the workflow structure is correct<br>
 * 2. Runtime validation - validates resolved configurations against schemas
 */
@Service
@AllArgsConstructor
@Slf4j
public class WorkflowValidationService {
    private final static String WORKFLOW_STRUCTURE_SCHEMA_NAME = "workflow_structure_schema";
    // Regex to validate alias keys. Allows alphanumeric characters, underscores, and hyphens.
    // Must start and end with an alphanumeric character.
    private final static Pattern aliasPattern = Pattern.compile("^[a-zA-Z0-9]+([a-zA-Z0-9_-]*[a-zA-Z0-9]+)?$");
    private final SchemaValidationService schemaValidationService;
    private final WorkflowDependencyValidator workflowDependencyValidator;
    private final SchemaTemplateValidationService schemaTemplateValidationService;
    private final PluginNodeExecutorRegistry executorRegistry;

    /**
     * Phase 1: Validates a workflow definition against schema requirements.
     * Performs structural validation, node configuration validation, and
     * validates node references within the workflow.
     *
     * @param workflow The workflow definition to validate
     * @return ValidationResult containing any validation errors found
     */
    public ValidationResult validateDefinition(WorkflowDefinition workflow) {
        List<ValidationError> errors = new ArrayList<>();

        // Validate overall workflow structure
        errors.addAll(schemaValidationService.validateAgainstSchema(
                null, workflow, String.format("builtin:%s", WORKFLOW_STRUCTURE_SCHEMA_NAME), "", null));

        // Validate individual node configurations
        errors.addAll(validateNodeConfigurations(workflow));

        // Validate node references
        errors.addAll(validateNodeReferences(workflow));

        errors.addAll(workflowDependencyValidator.validateNodeDependencyLoops(workflow));

        errors.addAll(validateAliasKeys(workflow));

        return new ValidationResult("definition", errors);
    }

    private List<ValidationError> validateAliasKeys(WorkflowDefinition workflow) {
        List<ValidationError> errors = new ArrayList<>();
        if (workflow.metadata() == null || workflow.metadata().aliases() == null) {
            return errors;
        }

        for (String aliasName : workflow.metadata().aliases().keySet()) {
            if (!aliasPattern.matcher(aliasName).matches()) {
                errors.add(ValidationError.builder()
                        .nodeKey("aliases") // Alias is not tied to a specific node
                        .errorType("definition")
                        .errorCode(ValidationErrorCode.INVALID_ALIAS_FORMAT)
                        .path("metadata.aliases." + aliasName)
                        .message("Invalid alias name: '" + aliasName + "'. Alias names must be alphanumeric and may contain hyphens or underscores, but cannot start or end with them.")
                        .value(aliasName)
                        .build());
            }
        }
        return errors;
    }

    /**
     * Phase 2: Validates node configurations at runtime with resolved values.
     * Ensures that after template resolution, the configuration still meets schema requirements.
     *
     * @param nodeKey        The key of the node being validated
     * @param resolvedConfig The resolved configuration with all templates expanded
     * @param templateString Template string formats:
     *                       <ul>
     *                         <li>Built-in: <code>builtin:&#60;name&#62;</code> (e.g., <code>builtin:http-trigger</code>)</li>
     *                         <li>Plugin: <code>&#60;nodeKey&#62;</code> (e.g., <code>123e4567-e89b-12d3-a456-426614174001</code>)</li>
     *                       </ul>
     * @return ValidationResult containing any runtime validation errors found
     */
    public ValidationResult validateRuntime(String nodeKey, WorkflowConfig resolvedConfig, String templateString, ExecutionContext context) {
        List<ValidationError> errors = new ArrayList<>();

        try {
            if (templateString != null && !templateString.isEmpty() && resolvedConfig != null) {
                errors.addAll(schemaValidationService.validateAgainstSchema(
                        nodeKey, resolvedConfig, templateString, nodeKey + ".config.input", "input"));
            }

            PluginNodeIdentifier identifier = null;
            try {
                identifier = PluginNodeIdentifier.fromString(templateString);
            } catch (Exception ignored) {
            }

            // Additional runtime-specific validations
            errors.addAll(validateRuntimeConstraints(nodeKey, resolvedConfig, identifier, context));

        } catch (Exception e) {
            errors.add(ValidationError.builder()
                    .nodeKey(nodeKey)
                    .errorType("runtime")
                    .errorCode(ValidationErrorCode.RUNTIME_VALIDATION_FAILED)
                    .path(nodeKey)
                    .message("Runtime validation failed: " + e.getMessage())
                    .build());
        }

        return new ValidationResult("runtime", errors);
    }

    /**
     * Validates the configuration of each node in the workflow.
     * Ensures that node configurations adhere to their respective schemas.
     *
     * @param workflow The workflow definition containing nodes to validate
     * @return List of validation errors found in node configurations
     */
    private List<ValidationError> validateNodeConfigurations(WorkflowDefinition workflow) {
        List<ValidationError> errors = new ArrayList<>();

        for (BaseWorkflowNode node : workflow.nodes()) {
            log.debug("Processing node: {} (type: {})", node.getKey(), node.getType());

            validatePluginNode(node, errors);
            PluginNodeIdentifier identifier = node.getPluginNode();
            String templateString = identifier.toCacheKey();
            log.debug("Plugin node detected - templateString: {}", templateString);

            // This validates the structure and static values against the schema while skipping template fields
            if (node.getConfig() != null) {
                log.debug("Validating schema structure for node: {}", node.getKey());
                List<ValidationError> schemaErrors = schemaValidationService.validateAgainstSchema(
                        node.getKey(),
                        node.getConfig(),
                        templateString,
                        node.getKey() + ".config.input",
                        "input",
                        true  // Skip template fields during definition phase
                );
                log.debug("Found {} schema errors for node: {} (template fields excluded)", schemaErrors.size(), node.getKey());
                errors.addAll(schemaErrors);
            }

            executorRegistry.getExecutor(identifier).ifPresent(executor -> {
                List<ValidationError> defErrors = executor.validateDefinition(node.getConfig());
                if (defErrors != null) {
                    defErrors.forEach(error -> {
                        if (error.getNodeKey() == null) {
                            error.setNodeKey(node.getKey());
                        }
                        if (error.getErrorType() == null) {
                            error.setErrorType("definition");
                        }
                        errors.add(error);
                    });
                }
            });

            // Validate template references in the node's configuration
            if (node.getConfig().input() != null) {
                Set<String> templates = TemplateEngine.extractRefs(node.getConfig());
                Map<String, OutputUsage> nodeConsumers = workflow.metadata() != null ?
                        workflow.metadata().nodeConsumers() : null;

                if (nodeConsumers != null && !templates.isEmpty()) {
                    log.debug("Validating template references for node: {}", node.getKey());
                    List<ValidationError> templateErrors = schemaTemplateValidationService.validateTemplateType(
                            node.getKey(),
                            templateString,
                            node.getConfig().input(),
                            workflow.metadata(),
                            templates
                    );
                    log.debug("Found {} template errors for node: {}", templateErrors.size(), node.getKey());
                    errors.addAll(templateErrors);
                }
            }
        }

        return errors;
    }


    /**
     * Validates a plugin node's configuration against its schema.
     * Checks that the plugin node has required fields and valid configuration.
     *
     * @param node   The plugin node to validate
     * @param errors List to collect validation errors
     */
    private void validatePluginNode(BaseWorkflowNode node, List<ValidationError> errors) {
        if (node.getPluginNode() == null) {
            errors.add(ValidationError.builder()
                    .nodeKey(node.getKey())
                    .errorType("definition")
                    .errorCode(ValidationErrorCode.PLUGIN_NODE_DEFINITION_MISSING)
                    .path(node.getKey())
                    .message("Plugin node definition is missing! Require pluginKey and nodeKey!")
                    .build());
            return;
        }

        // Validate plugin-specific configuration
        if (node.getConfig() == null) {
            errors.add(ValidationError.builder()
                    .nodeKey(node.getKey())
                    .errorType("definition")
                    .errorCode(ValidationErrorCode.PLUGIN_NODE_CONFIG_MISSING)
                    .path(node.getKey() + ".config")
                    .message("Plugin node configuration is missing")
                    .build());
        }

        errors.addAll(
                schemaValidationService.validateTemplates(
                        node.getKey(), node.getConfig(), node.getKey() + ".config.input"
                )
        );
    }

    /**
     * Validates that node references within the workflow point to existing nodes.
     * Checks both template references and explicit 'next' references.
     *
     * @param workflow The workflow definition to check for valid node references
     * @return List of validation errors for invalid node references
     */
    private List<ValidationError> validateNodeReferences(WorkflowDefinition workflow) {
        List<ValidationError> errors = new ArrayList<>();

        Set<String> nodeKeys = workflow.nodes().stream()
                .map(BaseWorkflowNode::getKey)
                .collect(Collectors.toSet());

        for (BaseWorkflowNode node : workflow.nodes()) {
            errors.addAll(validateNodeExistence(node, nodeKeys, workflow.metadata().aliases()));
        }

        return errors;
    }

    /**
     * Validates that the node's configuration does not reference non-existent nodes.
     * Checks both template references and explicit 'next' references.
     *
     * @param node     The workflow node to validate
     * @param nodeKeys Set of all existing node keys in the workflow
     * @param aliases  Map of aliases defined in the workflow metadata
     * @return List of validation errors for missing node references
     */
    private List<ValidationError> validateNodeExistence(BaseWorkflowNode node, Set<String> nodeKeys,
                                                        Map<String, String> aliases) {
        List<ValidationError> errors = new ArrayList<>();

        if (node.getConfig() != null) {
            Set<String> templates = TemplateEngine.extractRefs(node.getConfig());

            for (String template : templates) {
                String referencedNode = TemplateEngine.getReferencedNode(template, aliases);

                // Only validate existence - dependency direction is handled by WorkflowDependencyValidator
                if (referencedNode != null && !nodeKeys.contains(referencedNode)) {
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
        }

        return errors;
    }

    /**
     * Validates runtime-specific constraints based on the node type.
     * Applies different validation rules depending on the type of node.
     *
     * @param nodeKey        The key of the node being validated
     * @param resolvedConfig The resolved configuration with all templates expanded
     * @param executorIdentifier The identifier for the plugin node executor
     * @param context        The runtime context for validation
     * @return List of validation errors found during runtime constraint validation
     */
    private List<ValidationError> validateRuntimeConstraints(String nodeKey,
                                                             WorkflowConfig resolvedConfig,
                                                             PluginNodeIdentifier executorIdentifier,
                                                             ExecutionContext context) {
        List<ValidationError> errors = new ArrayList<>();

        // Add specific runtime validations based on a node type
        if (executorIdentifier != null && "core:flow.branch.condition:1.0.0".equals(executorIdentifier.toCacheKey())) {
            errors.addAll(validateConditionNodeRuntime(nodeKey, resolvedConfig));
        }

        if (executorIdentifier != null) {
            executorRegistry.getExecutor(executorIdentifier).ifPresent(executor -> {
                List<ValidationError> runtimeErrors = executor.validateRuntime(resolvedConfig, context);
                if (runtimeErrors != null) {
                    runtimeErrors.forEach(error -> {
                        if (error.getNodeKey() == null) {
                            error.setNodeKey(nodeKey);
                        }
                        if (error.getErrorType() == null) {
                            error.setErrorType("runtime");
                        }
                        errors.add(error);
                    });
                }
            });
        }

        return errors;
    }

    /**
     * Performs runtime validation specific to condition nodes.
     * Ensures that conditions have properly resolved cases after template expansion.
     *
     * @param nodeKey        The key of the condition node
     * @param resolvedConfig The resolved configuration for the condition node
     * @return List of validation errors specific to condition node runtime validation
     */
    private List<ValidationError> validateConditionNodeRuntime(String nodeKey, WorkflowConfig resolvedConfig) {
        List<ValidationError> errors = new ArrayList<>();

        Object input = resolvedConfig.input();
        if (input != null) {
            Map<String, Object> inputMap = ObjectConversion.convertObjectToMap(input);

            // Validate that case's array is not empty after resolution
            Object cases = inputMap.get("cases");
            if (cases instanceof List<?> casesList) {
                if (casesList.isEmpty()) {
                    errors.add(ValidationError.builder()
                            .nodeKey(nodeKey)
                            .errorType("runtime")
                            .errorCode(ValidationErrorCode.EMPTY_CASES_IN_CONDITION_NODE)
                            .path(nodeKey + ".input.cases")
                            .message("Cases array cannot be empty after template resolution")
                            .value(cases)
                            .build());
                }
            }

            // Add more runtime-specific validations as needed
        }

        return errors;
    }
}
