package org.phong.zenflow.plugin.subdomain.executors.builtin.database.base;

import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.exceptions.ExecutorException;
import org.phong.zenflow.plugin.subdomain.executors.builtin.database.dto.ResolvedDbConfig;
import org.phong.zenflow.workflow.subdomain.node_logs.utils.LogCollector;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class BaseSqlExecutor{
    public ExecutionResult execute(ResolvedDbConfig config) {
        LogCollector logCollector = new LogCollector();
        try {
            logCollector.info("Executing " + config.getDriver() + " query with config: " + config);
            Map<String, Object> result = executeGeneric(config, logCollector);

            return ExecutionResult.success(result, logCollector.getLogs());
        } catch (Exception e) {
            logCollector.error("Error executing " + config.getDriver() + " query: " + e.getMessage());
            return ExecutionResult.error("Execution error: " + e.getMessage(), logCollector.getLogs());
        }
    }

    private Map<String, Object> executeGeneric(ResolvedDbConfig config, LogCollector logCollector) {
        String query = config.getQuery();
        boolean enableTransaction = (boolean) config.getParams().getOrDefault("enableTransaction", false);

        try (Connection conn = config.getDataSource().getConnection()) {
            return prepareAndExecute(config, logCollector, conn, query, enableTransaction);
        } catch (SQLException e) {
            logCollector.error("SQL error: " + e.getMessage(), e);
            throw new ExecutorException("SQL error: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> prepareAndExecute(ResolvedDbConfig config, LogCollector logCollector, Connection conn, String query, boolean enableTransaction) {
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            logCollector.info("Preparing statement: " + query);
            Instant start = Instant.now();

            if (enableTransaction) {
                conn.setAutoCommit(false);
            }
            boolean isResult = stmt.execute();
            if (enableTransaction) {
                conn.commit();
            }
            Instant end = Instant.now();
            logCollector.info("Execution time: " + Duration.between(start, end).toMillis() + " ms");

            return getOutputResult(logCollector, query, isResult, stmt, start, end);
        } catch (Exception e) {
            if (enableTransaction && conn != null) {
                try {
                    logCollector.info("Rolling back transaction due to error: " + e.getMessage());
                    config.getDataSource().getConnection().rollback();
                } catch (Exception rollbackEx) {
                    logCollector.error("Rollback failed: " + rollbackEx.getMessage(), rollbackEx);
                }
            }
            logCollector.error("Query execution failed: " + e.getMessage(), e);
            throw new ExecutorException("Query execution failed: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> getOutputResult(LogCollector logCollector, String query, boolean isResult, PreparedStatement stmt, Instant start, Instant end) throws Exception {
        if (isResult) {
            ResultSet resultSet = stmt.getResultSet();
            List<Object> results = extractRows(resultSet);

            return Map.of(
                    "query", query,
                    "executionTime", getExecutionTime(start, end),
                    "rowCount", results.size(),
                    "results", results
            );
        } else {
            int affectedRows = stmt.getUpdateCount();
            logCollector.info("Query executed successfully, affected rows: " + affectedRows);

            return Map.of(
                    "query", query,
                    "executionTime", getExecutionTime(start, end),
                    "affectedRows", affectedRows
            );
        }
    }

    private List<Object> extractRows(ResultSet rs) throws Exception {
        List<Object> results = new ArrayList<>();
        int colCount = rs.getMetaData().getColumnCount();

        while (rs.next()) {
            Map<String, Object> row = new HashMap<>();
            for (int i = 1; i <= colCount; i++) {
                row.put(rs.getMetaData().getColumnLabel(i), rs.getObject(i));
            }
            results.add(row);
        }

        return results;
    }

    private String getExecutionTime(Instant start, Instant end) {
        return Duration.between(start, end).toMillis() + " ms";
    }
}
