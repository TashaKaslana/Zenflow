package org.phong.zenflow.plugin.subdomain.executors.builtin.database.pool;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.phong.zenflow.plugin.subdomain.executors.builtin.database.dto.DbConnectionKey;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class GlobalDbConnectionPool {
    private final Cache<DbConnectionKey, HikariDataSource> poolCache = Caffeine.newBuilder()
            .maximumSize(500)
            .expireAfterAccess(Duration.ofMinutes(10))
            .build();

    public HikariDataSource getOrCreate(DbConnectionKey key, String password) {
        return poolCache.get(key, k -> createDataSource(k, password));
    }

    private HikariDataSource createDataSource(DbConnectionKey key, String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:" + key.getDatabaseSource());
        config.setUsername(key.getUsername());
        config.setPassword(password);
        return new HikariDataSource(config);
    }
}

