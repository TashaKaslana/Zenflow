package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.database.base;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.subdomain.execution.exceptions.ExecutorException;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.database.base.dto.DbConnectionKey;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.database.base.dto.ResolvedDbConfig;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.database.base.pool.GlobalDbConnectionPool;
import org.phong.zenflow.plugin.subdomain.resource.ScopedNodeResource;
import com.zaxxer.hikari.HikariDataSource;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.config.WorkflowConfig;
import org.phong.zenflow.workflow.subdomain.logging.core.NodeLogPublisher;
import org.springframework.stereotype.Component;

import java.util.Map;

@AllArgsConstructor
@Slf4j
@Component
public class BaseDbConnection {
    private final GlobalDbConnectionPool globalPool;

    public ResolvedDbConfig establishConnection(WorkflowConfig config, ExecutionContext context) {
        NodeLogPublisher logPublisher = context.getLogPublisher();
        try {
            logPublisher.info("Executing DB node with config: {}", config);
            Map<String, Object> input = config.input();

            ResolvedDbConfig dbConfig = ResolvedDbConfig.fromInput(input);
            String connectionId = dbConfig.getConnectionIdOrGenerate();
            DbConnectionKey key = dbConfig.toConnectionKey();
            GlobalDbConnectionPool.DbConfig dbPoolConfig = new GlobalDbConnectionPool.DbConfig(
                    dbConfig.toConnectionKey(), dbConfig.getPassword()
            );

            logPublisher.info("Creating new DataSource for connectionId: {}", connectionId);
            try (ScopedNodeResource<HikariDataSource> handle = globalPool.acquire(key.toString(), context.getWorkflowRunId(), dbPoolConfig)) {
                dbConfig.setDataSource(handle.getResource());
            }
            logPublisher.info("Using DataSource for connectionId: {}", connectionId);

            return dbConfig;
        } catch (Exception e) {
            log.error("DB execution failed", e);
            logPublisher.withException(e).error("DB execution failed: {}", e.getMessage());
            throw new ExecutorException("Failed to establish DB connection: " + e.getMessage(), e);
        }
    }
}

