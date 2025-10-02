package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.database.base;

import org.phong.zenflow.plugin.subdomain.execution.dto.ExecutionResult;
import org.phong.zenflow.plugin.subdomain.execution.exceptions.ExecutorException;
import org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.database.base.dto.ResolvedDbConfig;
import org.phong.zenflow.workflow.subdomain.logging.core.NodeLogPublisher;
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
        void bind(PreparedStatement stmt, ResolvedDbConfig config, NodeLogPublisher log, AtomicBoolean isBatch) throws SQLException;
    }

    @FunctionalInterface
    public interface ResultProcessor {
        Map<String, Object> process(Map<String, Object> result, ResolvedDbConfig config, NodeLogPublisher log);
    }

    public ExecutionResult execute(ResolvedDbConfig config, NodeLogPublisher nodeLog) {
        return execute(config, nodeLog, null, null);
    }

    public ExecutionResult execute(ResolvedDbConfig config, NodeLogPublisher nodeLog,
                                   ParameterBinder parameterBinder, ResultProcessor resultProcessor) {
        try {
            nodeLog.info("Executing {} query with config: {}", config.getDriver(), config);
            Map<String, Object> result = executeGeneric(config, nodeLog, parameterBinder);

            if (resultProcessor != null) {
                result = resultProcessor.process(result, config, nodeLog);
            }

            return ExecutionResult.success(result);
        } catch (Exception e) {
            nodeLog.withException(e).error("Error executing {} query: {}", config.getDriver(), e.getMessage());
            throw new ExecutorException("Execution error: " + e.getMessage());
        }
    }

    private Map<String, Object> executeGeneric(ResolvedDbConfig config, NodeLogPublisher nodeLog, ParameterBinder parameterBinder) {
        String query = config.getQuery();
        boolean enableTransaction = config.getParams() != null && (boolean) config.getParams().getOrDefault("enableTransaction", false);

        try (Connection conn = config.getDataSource().getConnection()) {
            return prepareAndExecute(config, nodeLog, conn, query, enableTransaction, parameterBinder);
        } catch (SQLException e) {
            nodeLog.withException(e).error("SQL error: {}", e.getMessage());
            throw new ExecutorException("SQL error: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> prepareAndExecute(ResolvedDbConfig config, NodeLogPublisher nodeLog, Connection conn,
                                                  String query, boolean enableTransaction, ParameterBinder parameterBinder) {
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            nodeLog.info("Preparing statement: {}", query);
            Instant start = Instant.now();
            AtomicBoolean isBatch = new AtomicBoolean(false);

            // Apply custom parameter binding if provided
            if (parameterBinder != null) {
                parameterBinder.bind(stmt, config, nodeLog, isBatch);
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
            nodeLog.info("Execution time: {} ms", Duration.between(start, end).toMillis());

            return getOutputResult(nodeLog, query, isResult, stmt, start, end, affectedRows, batchCounts);
        } catch (Exception e) {
            if (enableTransaction && conn != null) {
                try {
                    nodeLog.info("Rolling back transaction due to error: {}", e.getMessage());
                    conn.rollback();
                } catch (SQLException rollbackEx) {
                    nodeLog.withException(rollbackEx).error("Rollback failed: {}", rollbackEx.getMessage());
                }
            }
            nodeLog.withException(e).error("Query execution failed: {}", e.getMessage());
            throw new ExecutorException("Query execution failed: " + e.getMessage(), e);
        }
    }

    private Map<String, Object> getOutputResult(NodeLogPublisher nodeLog, String query, boolean isResult,
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
            nodeLog.info("Query executed successfully, affected rows: {}", affectedRows);

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
