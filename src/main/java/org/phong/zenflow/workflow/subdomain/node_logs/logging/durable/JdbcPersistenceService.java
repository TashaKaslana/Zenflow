package org.phong.zenflow.workflow.subdomain.node_logs.logging.durable;// logging/collector/JdbcPersistenceService.java
import org.phong.zenflow.workflow.subdomain.node_logs.logging.LogEntry;

import javax.sql.DataSource;
import java.sql.Connection; import java.sql.PreparedStatement; import java.sql.Types;
import java.time.ZoneOffset; import java.util.List; import java.util.UUID;

public class JdbcPersistenceService implements PersistenceService {
    private final DataSource ds;
    public JdbcPersistenceService(DataSource ds){ this.ds = ds; }

    @Override
    public void saveBatch(UUID runId, List<LogEntry> entries) throws Exception {
        // Postgres schema assumed; adapt as needed
//        String sql = """
//            INSERT INTO logs(workflow_id, run_id, node_key, ts, level, message, error_code, metadata, trace_id, hierarchy)
//            VALUES (?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?, ?)
//        """;
//        try(Connection c = ds.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
//            for(LogEntry e : entries){
//                ps.setObject(1, e.workflowId);
//                ps.setObject(2, e.runId);
//                ps.setString(3, e.nodeKey);
//                ps.setObject(4, e.timestamp.atOffset(ZoneOffset.UTC));
//                ps.setString(5, e.level.name());
//                ps.setString(6, e.message);
//                if(e.errorCode == null) ps.setNull(7, Types.VARCHAR); else ps.setString(7, e.errorCode);
//                String json = (e.meta==null || e.meta.isEmpty()) ? "{}" : Jsons.stringify(e.meta); // implement Jsons
//                ps.setString(8, json);
//                ps.setString(9, e.traceId);
//                ps.setString(10, e.hierarchy);
//                ps.addBatch();
//            }
//            ps.executeBatch();
//        }
    }
}
