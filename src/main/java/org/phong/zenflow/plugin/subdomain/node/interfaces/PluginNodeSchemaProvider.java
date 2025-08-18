package org.phong.zenflow.plugin.subdomain.node.interfaces;

import org.phong.zenflow.workflow.subdomain.node_definition.definitions.plugin.PluginNodeIdentifier;

import java.util.List;
import java.util.Map;

public interface PluginNodeSchemaProvider {
/**
     * Retrieves the schema JSON for a given plugin node identifier.
     *
     * @param identifier the plugin node identifier
     * @return a map representing the schema JSON
     */
    Map<String, Object> getSchemaJson(PluginNodeIdentifier identifier);

    /**
     * Retrieves all schema JSONs for a list of plugin node identifiers.
     *
     * @param identifiers the list of plugin node identifiers
     * @return a map where the key is the identifier and the value is the schema JSON map
     */
    Map<String, Map<String, Object>> getAllSchemasByIdentifiers(List<PluginNodeIdentifier> identifiers);

    /**
     * Gets schema directly from a file system for better performance.
     * This method bypasses database queries and loads schemas directly from JSON files.
     *
     * @param identifier The plugin node identifier
     * @return Schema map loaded directly from a file
     */
    Map<String, Object> getSchemaJsonFromFile(PluginNodeIdentifier identifier);

    /**
     * Gets multiple schemas directly from a file system in a batch.
     *
     * @param identifiers List of plugin node identifiers
     * @return Map of an identifier cache key to schema
     */
    Map<String, Map<String, Object>> getAllSchemasByIdentifiersFromFile(List<PluginNodeIdentifier> identifiers);
}
