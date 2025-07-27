package org.phong.zenflow.workflow.subdomain.schema_validator.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ValidationErrorCode {

    // === Definition Phase Errors ===
    SCHEMA_NOT_FOUND("Schema not found for validation"),
    INVALID_WORKFLOW_STRUCTURE("Workflow structure does not conform to the schema"),
    PLUGIN_NODE_DEFINITION_MISSING("Plugin node definition is missing required fields (pluginId, nodeId)"),
    PLUGIN_NODE_CONFIG_MISSING("Plugin node configuration is missing"),
    INVALID_PLUGIN_NODE_DEFINITION("Invalid plugin node definition"),
    MISSING_NODE_REFERENCE("Referenced node does not exist in the workflow"),
    CIRCULAR_DEPENDENCY("Workflow contains cycles - cannot determine execution order"),
    DUPLICATE_NODE_KEY("Node key is not unique"),
    INVALID_NODE_TYPE("Unrecognized or unsupported node type"),
    MISSING_REQUIRED_FIELD("Required field is missing in the node definition"),
    INVALID_SCHEMA("Node does not conform to the expected schema"),
    INVALID_CONNECTION("Invalid or unresolvable connection between nodes"),
    UNREACHABLE_NODE("Node is unreachable from the start node"),
    DISCONNECTED_GRAPH("Workflow has isolated or disconnected components"),
    INVALID_INPUT_STRUCTURE("Input definition is not well-formed"),

    // === Runtime Phase Errors ===
    RUNTIME_VALIDATION_FAILED("Runtime validation failed"),
    EMPTY_CASES_IN_CONDITION_NODE("Cases array in condition node cannot be empty after template resolution"),
    VALUE_RESOLUTION_FAILED("Failed to resolve value at runtime"),
    TYPE_MISMATCH("Resolved value does not match expected type"),
    MISSING_RUNTIME_CONTEXT("Runtime context is missing required variables"),
    EXECUTION_FAILURE("Node execution failed during runtime"),

    // === Template & Expression Errors ===
    INVALID_TEMPLATE_EXPRESSION("Invalid template expression syntax"),
    TEMPLATE_TYPE_VALIDATION_ERROR("Error validating template types"),
    TEMPLATE_SYNTAX_ERROR("Template expression syntax is invalid"),
    UNKNOWN_FUNCTION_IN_TEMPLATE("Template uses an unknown function"),
    TEMPLATE_EVALUATION_FAILED("Template could not be evaluated"),

    // === Scheduling / Timeout Errors ===
    SCHEDULE_PARSING_FAILED("Schedule expression could not be parsed"),
    TIMEOUT_CONFIGURATION_INVALID("Timeout settings are invalid or missing"),

    // === General Errors ===
    VALIDATION_ERROR("An error occurred during validation"),
    UNKNOWN_ERROR("An unknown error occurred"),
    UNSUPPORTED_OPERATION("This operation is not supported in current mode");

    private final String defaultMessage;
}
