package org.phong.zenflow.plugin.subdomain.executors.builtin.database.base;

import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionInput;
import org.phong.zenflow.workflow.subdomain.context.RuntimeContextPool;
import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.exceptions.ExecutorException;
import org.phong.zenflow.plugin.subdomain.executors.builtin.database.dto.ResolvedDbConfig;
import org.phong.zenflow.workflow.subdomain.node_logs.utils.LogCollector;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
public class BaseSqlExecutor {

    @FunctionalInterface
    public interface ParameterBinder {
        void bind(PreparedStatement stmt, ResolvedDbConfig config, LogCollector logCollector, AtomicBoolean isBatch) throws SQLException;
    }

    @FunctionalInterface
    public interface ResultProcessor {
        Map<String, Object> process(Map<String, Object> result, ResolvedDbConfig config, LogCollector logCollector);
    }

    public ExecutionResult execute(ResolvedDbConfig config) {
        LogCollector logCollector = new LogCollector();
        return execute(config, logCollector);
    }

    public ExecutionResult execute(ResolvedDbConfig config, LogCollector logCollector) {
        return execute(config, logCollector, null, null);
    }

    public ExecutionResult execute(ResolvedDbConfig config, LogCollector logCollector,
                                   ParameterBinder parameterBinder, ResultProcessor resultProcessor) {
        try {
            logCollector.info("Executing " + config.getDriver() + " query with config: " + config);
            Map<String, Object> result = executeGeneric(config, logCollector, parameterBinder);

            // Apply result processing if provided
            if (resultProcessor != null) {
                result = resultProcessor.process(result, config, logCollector);
            }

            return ExecutionResult.success(result, logCollector.getLogs());
        } catch (Exception e) {
            logCollector.error("Error executing " + config.getDriver() + " query: " + e.getMessage());
            return ExecutionResult.error("Execution error: " + e.getMessage(), logCollector.getLogs());
        }
    }

    private Map<String, Object> executeGeneric(ResolvedDbConfig config, LogCollector logCollector, ParameterBinder parameterBinder) {
        String query = config.getQuery();
        boolean enableTransaction = config.getParams() != null && (boolean) config.getParams().getOrDefault("enableTransaction", false);

        try (Connection conn = config.getDataSource().getConnection()) {
            return prepareAndExecute(config, logCollector, conn, query, enableTransaction, parameterBinder);
        } catch (SQLException e) {
            logCollector.error("SQL error: " + e.getMessage(), e);
            throw new ExecutorException("SQL error: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> prepareAndExecute(ResolvedDbConfig config, LogCollector logCollector, Connection conn,
                                                  String query, boolean enableTransaction, ParameterBinder parameterBinder) {
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            logCollector.info("Preparing statement: " + query);
            Instant start = Instant.now();
            AtomicBoolean isBatch = new AtomicBoolean(false);

            // Apply custom parameter binding if provided
            if (parameterBinder != null) {
                parameterBinder.bind(stmt, config, logCollector, isBatch);
            }

            if (enableTransaction) {
                conn.setAutoCommit(false);
            }

            boolean isResult;
            int[] batchCounts = null;
            int affectedRows = 0;
            if (isBatch.get()) {
                batchCounts = stmt.executeBatch();
                for (int count : batchCounts) {
                    affectedRows += count;
                }
                isResult = false; // Batch execution does not return a result set
            } else {
                isResult = stmt.execute();
                affectedRows = stmt.getUpdateCount();
            }
            if (enableTransaction) {
                conn.commit();
            }
            Instant end = Instant.now();
            logCollector.info("Execution time: " + Duration.between(start, end).toMillis() + " ms");

            return getOutputResult(logCollector, query, isResult, stmt, start, end, affectedRows, batchCounts);
        } catch (Exception e) {
            if (enableTransaction && conn != null) {
                try {
                    logCollector.info("Rolling back transaction due to error: " + e.getMessage());
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    logCollector.error("Rollback failed: " + rollbackEx.getMessage(), rollbackEx);
                }
            }
            logCollector.error("Query execution failed: " + e.getMessage(), e);
            throw new ExecutorException("Query execution failed: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> getOutputResult(LogCollector logCollector, String query, boolean isResult,
                                               PreparedStatement stmt, Instant start, Instant end,
                                               int affectedRows, int[] batchCounts) throws Exception {
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
            logCollector.info("Query executed successfully, affected rows: " + affectedRows);

            Map<String, Object> result = new HashMap<>();
            result.put("query", query);
            result.put("executionTime", getExecutionTime(start, end));
            result.put("affectedRows", affectedRows);
            if (batchCounts != null) {
                result.put("batchCounts", batchCounts);
            }
            return result;
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
