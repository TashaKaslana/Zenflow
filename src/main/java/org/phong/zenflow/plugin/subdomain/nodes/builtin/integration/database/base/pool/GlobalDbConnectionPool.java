package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.database.base.pool;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.database.base.dto.DbConnectionKey;
import org.phong.zenflow.plugin.subdomain.resource.BaseNodeResourceManager;
import org.phong.zenflow.plugin.subdomain.resource.ResourceConfig;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.config.WorkflowConfig;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Global pool for database connections following the {@link BaseNodeResourceManager}
 * pattern. Provides shared {@link HikariDataSource} instances keyed by
 * {@link DbConnectionKey}.
 */
@Component
public class GlobalDbConnectionPool extends BaseNodeResourceManager<HikariDataSource, GlobalDbConnectionPool.DbConfig> {
    @Override
    public DbConfig buildConfig(WorkflowConfig cfg, ExecutionContext ctx) {
        return null;
    }

    @Override
    protected HikariDataSource createResource(String resourceKey, DbConfig config) {
        return createDataSource(config.key(), config.password());
    }

    @Override
    protected void cleanupResource(HikariDataSource resource) {
        resource.close();
    }

    private HikariDataSource createDataSource(DbConnectionKey key, String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:" + key.getDatabaseSource());
        config.setUsername(key.getUsername());
        config.setPassword(password);
        config.setDriverClassName(getDriverClassName(key.getDriver()));
        return new HikariDataSource(config);
    }

    private String getDriverClassName(String driver) {
        return switch (driver.toLowerCase()) {
            case "postgresql" -> "org.postgresql.Driver";
            case "mysql" -> "com.mysql.cj.jdbc.Driver";
            case "oracle" -> "oracle.jdbc.OracleDriver";
            case "sqlserver" -> "com.microsoft.sqlserver.jdbc.SQLServerDriver";
            default -> null; // Let HikariCP auto-detect
        };
    }

    public record DbConfig(DbConnectionKey key, String password) implements ResourceConfig {

        @Override
        public String getResourceIdentifier() {
            return "";
        }

        @Override
        public Map<String, Object> getContextMap() {
            return Map.of();
        }
    }
}
