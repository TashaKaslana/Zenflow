package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.database.postgres;

import lombok.AllArgsConstructor;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinition;
import org.phong.zenflow.plugin.subdomain.node.definition.NodeDefinitionProvider;
import org.phong.zenflow.plugin.subdomain.node.registry.PluginNode;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.database.base.pool.GlobalDbConnectionPool;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@PluginNode(
        key = "integration:postgresql",
        name = "PostgreSQL",
        version = "1.0.0",
        description = "Executes SQL queries against a PostgreSQL database with advanced parameter handling and type inference.",
        type = "integration.database",
        icon = "postgresql",
        tags = { "database", "postgresql", "sql", "integration" }
)
public class PostgresSqlNode implements NodeDefinitionProvider {
    private final PostgresSqlExecutor executor;
    private final PostgresSqlNodeValidator validator;
    private final GlobalDbConnectionPool resourceManager;

    @Override
    public NodeDefinition definition() {
        return NodeDefinition.builder()
                .nodeExecutor(executor)
                .nodeValidator(validator)
                .nodeResourceManager(resourceManager)
                .build();
    }
}
