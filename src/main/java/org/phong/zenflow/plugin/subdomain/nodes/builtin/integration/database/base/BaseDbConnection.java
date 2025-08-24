package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.database.base;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.subdomain.execution.exceptions.ExecutorException;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.database.base.dto.DbConnectionKey;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.database.base.dto.ResolvedDbConfig;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.database.base.pool.GlobalDbConnectionPool;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto.WorkflowConfig;
import org.phong.zenflow.workflow.subdomain.logging.core.NodeLogPublisher;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.Map;

@AllArgsConstructor
@Slf4j
@Component
public class BaseDbConnection {
    private final GlobalDbConnectionPool globalPool;

    public static String buildDbKey(String connectionId) {
        return "_db:" + connectionId;
    }

    public ResolvedDbConfig establishConnection(WorkflowConfig config, ExecutionContext context) {
        NodeLogPublisher logPublisher = context.getLogPublisher();
        try {
            logPublisher.info("Executing DB node with config: {}", config);
            Map<String, Object> input = config.input();
            ResolvedDbConfig dbConfig = ResolvedDbConfig.fromInput(input);

            String connectionId = dbConfig.getConnectionIdOrGenerate();
            DataSource ds = getContextDataSource(context, connectionId);

            if (ds == null) {
                logPublisher.info("Creating new DataSource for connectionId: {}", connectionId);
                DbConnectionKey key = dbConfig.toConnectionKey();
                ds = globalPool.getOrCreate(key, dbConfig.getPassword());
                storeInContext(context, connectionId, ds);
            }

            dbConfig.setDataSource(ds);
            logPublisher.info("Using DataSource for connectionId: {}", connectionId);
            return dbConfig;
        } catch (Exception e) {
            log.error("DB execution failed", e);
            logPublisher.withException(e).error("DB execution failed: {}", e.getMessage());
            throw new ExecutorException("Failed to establish DB connection: " + e.getMessage(), e);
        }
    }

    private void storeInContext(ExecutionContext context, String connectionId, DataSource ds) {
        context.write(buildDbKey(connectionId), ds);
    }

    @Nullable
    private DataSource getContextDataSource(ExecutionContext context, String connectionId) {
        return context.read(buildDbKey(connectionId), DataSource.class);
    }
}
