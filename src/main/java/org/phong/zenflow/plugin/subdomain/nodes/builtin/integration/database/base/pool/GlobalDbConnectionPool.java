package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.database.base.pool;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.database.base.dto.DbConnectionKey;
import org.phong.zenflow.plugin.subdomain.resource.BaseNodeResourceManager;
import org.springframework.stereotype.Component;

/**
 * Global pool for database connections following the {@link BaseNodeResourceManager}
 * pattern. Provides shared {@link HikariDataSource} instances keyed by
 * {@link DbConnectionKey}.
 */
@Component
public class GlobalDbConnectionPool extends BaseNodeResourceManager<HikariDataSource, GlobalDbConnectionPool.DbConfig> {

    /**
     * Get or create a {@link HikariDataSource} for the given connection key.
     */
    public HikariDataSource getOrCreate(DbConnectionKey key, String password) {
        return getOrCreateResource(key.toString(), new DbConfig(key, password));
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

    public static record DbConfig(DbConnectionKey key, String password) {}
}
