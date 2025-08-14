package org.phong.zenflow.plugin.subdomain.executors.builtin.database.base;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.subdomain.execution.exceptions.ExecutorException;
import org.phong.zenflow.plugin.subdomain.executors.builtin.database.dto.DbConnectionKey;
import org.phong.zenflow.plugin.subdomain.executors.builtin.database.dto.ResolvedDbConfig;
import org.phong.zenflow.plugin.subdomain.executors.builtin.database.pool.GlobalDbConnectionPool;
import org.phong.zenflow.workflow.subdomain.context.RuntimeContext;
import org.phong.zenflow.workflow.subdomain.context.RuntimeContextPool;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto.WorkflowConfig;
import org.phong.zenflow.workflow.subdomain.node_logs.utils.LogCollector;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.Map;
import java.util.UUID;

@AllArgsConstructor
@Slf4j
@Component
public class BaseDbConnection {
    private final GlobalDbConnectionPool globalPool;

    public static String buildDbKey(String connectionId) {
        return "_db:" + connectionId;
    }

    public ResolvedDbConfig establishConnection(WorkflowConfig config, UUID workflowRunId, LogCollector logCollector) {
        try {
            logCollector.info("Executing DB node with config: " + config);
            Map<String, Object> input = config.input();
            ResolvedDbConfig dbConfig = ResolvedDbConfig.fromInput(input);

            RuntimeContext context = RuntimeContextPool.getContext(workflowRunId);

            String connectionId = dbConfig.getConnectionIdOrGenerate();
            DataSource ds = getContextDataSource(context, connectionId);

            if (ds == null) {
                logCollector.info("Creating new DataSource for connectionId: " + connectionId);
                DbConnectionKey key = dbConfig.toConnectionKey();
                ds = globalPool.getOrCreate(key, dbConfig.getPassword());
                storeInContext(context, connectionId, ds);
            }

            dbConfig.setDataSource(ds);
            logCollector.info("Using DataSource for connectionId: " + connectionId);
            return dbConfig;
        } catch (Exception e) {
            log.error("DB execution failed", e);
            logCollector.error("DB execution failed: " + e.getMessage(), e);
            throw new ExecutorException("Failed to establish DB connection: " + e.getMessage(), e);
        }
    }

    private void storeInContext(RuntimeContext context, String connectionId, DataSource ds) {
        context.put(buildDbKey(connectionId), ds);
    }

    @Nullable
    private DataSource getContextDataSource(RuntimeContext context, String connectionId) {
        return (DataSource) context.get(buildDbKey(connectionId));
    }
}
