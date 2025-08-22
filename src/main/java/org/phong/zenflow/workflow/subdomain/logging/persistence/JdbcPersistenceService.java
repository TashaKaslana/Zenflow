package org.phong.zenflow.workflow.subdomain.logging.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.phong.zenflow.workflow.subdomain.logging.core.LogEntry;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

public class JdbcPersistenceService implements PersistenceService {
    private final DataSource ds;
    private final ObjectMapper objectMapper;

    public JdbcPersistenceService(DataSource ds) {
        this.ds = ds;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public void saveBatch(UUID runId, List<LogEntry> entries) throws SQLException {
        String sql = "INSERT INTO node_logs(workflow_id, workflow_run_id, node_key, \"timestamp\", level, message, error_code, error_message, meta, trace_id, hierarchy, user_id, correlation_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?, ?, ?)";

        try (Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            for (LogEntry e : entries) {
                ps.setObject(1, e.getWorkflowId());
                ps.setObject(2, e.getWorkflowRunId());
                ps.setString(3, e.getNodeKey());
                ps.setObject(4, e.getTimestamp().atOffset(ZoneOffset.UTC));
                ps.setString(5, e.getLevel().name());
                ps.setString(6, e.getMessage());
                ps.setString(7, e.getErrorCode());
                ps.setString(8, e.getErrorMessage());
                try {
                    String metaJson = e.getMeta() == null ? null : objectMapper.writeValueAsString(e.getMeta());
                    ps.setString(9, metaJson);
                } catch (JsonProcessingException ex) {
                    throw new SQLException("Failed to serialize meta field", ex);
                }
                ps.setString(10, e.getTraceId());
                ps.setString(11, e.getHierarchy());
                ps.setObject(12, e.getUserId());
                ps.setString(13, e.getCorrelationId());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }
}