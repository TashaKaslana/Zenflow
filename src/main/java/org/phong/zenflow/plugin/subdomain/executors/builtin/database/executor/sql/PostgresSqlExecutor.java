package org.phong.zenflow.plugin.subdomain.executors.builtin.database.executor.sql;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.interfaces.PluginNodeExecutor;
import org.phong.zenflow.plugin.subdomain.executors.builtin.database.base.BaseDbConnection;
import org.phong.zenflow.plugin.subdomain.executors.builtin.database.base.BaseSqlExecutor;
import org.phong.zenflow.plugin.subdomain.executors.builtin.database.dto.ResolvedDbConfig;
import org.phong.zenflow.workflow.subdomain.context.RuntimeContext;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.dto.WorkflowConfig;
import org.phong.zenflow.workflow.subdomain.node_logs.utils.LogCollector;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@AllArgsConstructor
public class PostgresSqlExecutor implements PluginNodeExecutor {
    private final BaseDbConnection baseDbConnection;
    private final BaseSqlExecutor baseSqlExecutor;

    @Override
    public String key() {
        return "core:postgresql:1.0.0";
    }

    @Override
    public ExecutionResult execute(WorkflowConfig config, RuntimeContext context) {
        LogCollector logCollector = new LogCollector();
        try {
            log.info("Executing Postgres SQL node with config: {}", config);
            ResolvedDbConfig dbConfig = baseDbConnection.establishConnection(config, context, logCollector);
            return baseSqlExecutor.execute(dbConfig);
        } catch (Exception e) {
            log.error("Postgres SQL execution failed", e);
            return ExecutionResult.error("Postgres SQL execution failed: " + e.getMessage(), null);
        }
    }
}
