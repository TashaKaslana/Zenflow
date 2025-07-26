package org.phong.zenflow.workflow.subdomain.schema_validator.service;

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.core.utils.ObjectConversion;
import org.phong.zenflow.plugin.subdomain.execution.utils.TemplateEngine;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.BaseWorkflowNode;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.WorkflowDefinition;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto.WorkflowConfig;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.plugin.PluginDefinition;
import org.phong.zenflow.workflow.subdomain.node_definition.enums.NodeType;
import org.phong.zenflow.workflow.subdomain.schema_validator.dto.ValidationError;
import org.phong.zenflow.workflow.subdomain.schema_validator.dto.ValidationResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private final SchemaValidationService schemaValidationService;
    private final WorkflowDependencyValidator workflowDependencyValidator;
    private final SchemaTemplateValidationService schemaTemplateValidationService;

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
                workflow, String.format("builtin:%s", WORKFLOW_STRUCTURE_SCHEMA_NAME), ""));

        // Validate individual node configurations
        errors.addAll(validateNodeConfigurations(workflow));

        // Validate node references
        errors.addAll(validateNodeReferences(workflow));

        errors.addAll(workflowDependencyValidator.validateNodeDependencyLoops(workflow));

        return new ValidationResult("definition", errors);
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
     *                         <li>Plugin: <code>&#60;nodeId&#62;</code> (e.g., <code>123e4567-e89b-12d3-a456-426614174001</code>)</li>
     *                       </ul>
     * @return ValidationResult containing any runtime validation errors found
     */
    public ValidationResult validateRuntime(String nodeKey, WorkflowConfig resolvedConfig, String templateString) {
        List<ValidationError> errors = new ArrayList<>();

        try {
            if (templateString != null && !templateString.isEmpty() && resolvedConfig != null) {
                errors.addAll(schemaValidationService.validateAgainstSchema(
                        resolvedConfig, templateString, nodeKey + ".config"));
            }

            // Additional runtime-specific validations
            errors.addAll(validateRuntimeConstraints(nodeKey, resolvedConfig, templateString));

        } catch (Exception e) {
            errors.add(ValidationError.builder()
                    .type("runtime")
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
            String templateString = null;
            //Only handle plugin nodes for now
            if (node.getType() == NodeType.PLUGIN && node instanceof PluginDefinition pluginNode) {
                validatePluginNode(pluginNode, errors);
                templateString = pluginNode.getPluginNode().nodeId().toString();
            }

            //validate template references in the node's configuration
            if (templateString != null) {
                Set<String> templates = TemplateEngine.extractRefs(node.getConfig());
                Map<String, Object> nodeConsumers = ObjectConversion.convertObjectToMap(workflow.metadata().get("nodeConsumers"));
                errors.addAll(
                        schemaTemplateValidationService.validateTemplateType(
                                node.getKey(), templateString, nodeConsumers, templates
                        )
                );
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
        if (node instanceof PluginDefinition pluginNode) {
            if (pluginNode.getPluginNode() == null) {
                errors.add(ValidationError.builder()
                        .type("definition")
                        .path(node.getKey())
                        .message("Plugin node definition is missing! Require pluginId and nodeId!")
                        .build());
                return;
            }
            // Validate plugin-specific configuration
            if (pluginNode.getConfig() != null) {
                String schemaName = pluginNode.getPluginNode().nodeId().toString();

                errors.addAll(schemaValidationService.validateAgainstSchema(
                        pluginNode.getConfig(), schemaName, node.getKey() + ".config.input")
                );
            } else {
                errors.add(ValidationError.builder()
                        .type("definition")
                        .path(node.getKey() + ".config")
                        .message("Plugin node configuration is missing")
                        .build());
            }
        } else {
            errors.add(ValidationError.builder()
                    .type("definition")
                    .path(node.getKey())
                    .message("Invalid plugin node definition")
                    .build());
        }
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
            errors.addAll(validateNodeExistence(node, nodeKeys, workflow.metadata()));
        }

        return errors;
    }

    private List<ValidationError> validateNodeExistence(BaseWorkflowNode node, Set<String> nodeKeys,
                                                        Map<String, Object> metadata) {
        List<ValidationError> errors = new ArrayList<>();

        if (node.getConfig() != null) {
            Set<String> templates = TemplateEngine.extractRefs(node.getConfig());
            Map<String, String> aliases = ObjectConversion.safeConvert(metadata.get("alias"), new TypeReference<>() {
            });

            for (String template : templates) {
                String referencedNode = TemplateEngine.getReferencedNode(template, aliases);

                // Only validate existence - dependency direction is handled by WorkflowDependencyValidator
                if (referencedNode != null && !nodeKeys.contains(referencedNode)) {
                    errors.add(ValidationError.builder()
                            .type("MISSING_NODE_REFERENCE")
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
     * @param nodeType       The type of node being validated
     * @return List of validation errors found during runtime constraint validation
     */
    private List<ValidationError> validateRuntimeConstraints(String nodeKey, WorkflowConfig resolvedConfig, String nodeType) {
        List<ValidationError> errors = new ArrayList<>();

        // Add specific runtime validations based on a node type
        if ("conditionNode".equals(nodeType)) {
            errors.addAll(validateConditionNodeRuntime(nodeKey, resolvedConfig));
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
                            .type("runtime")
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
