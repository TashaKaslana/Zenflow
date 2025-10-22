package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.database.base.dto;

import lombok.Data;
import org.phong.zenflow.core.utils.ObjectConversion;
import org.phong.zenflow.workflow.subdomain.context.ExecutionContext;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Data
public class ResolvedDbConfig {
    private String driver;
    private String host;
    private int port;
    private String database;
    private String username;
    private String password;
    private String connectionId;
    private String query;
    private String returnType;
    private Map<String, Object> params;

    private DataSource dataSource;

    public static ResolvedDbConfig fromInput(ExecutionContext context) {
        ResolvedDbConfig cfg = new ResolvedDbConfig();
        cfg.driver = (String) context.read("driver", String.class); // e.g. "postgresql"
        cfg.host = (String) context.read("host", String.class);
        cfg.port = (int) context.read("port", Integer.class);
        cfg.database = (String) context.read("database", String.class);
        cfg.username = (String) context.read("username", String.class);
        cfg.password = (String) context.read("password", String.class);
        cfg.query = (String) context.read("query", String.class);
        cfg.returnType = (String) context.read("returnType", String.class);

        // Handle params extraction - support both nested and direct structure
        Object paramsObj = context.read("params", Object.class);
        if (paramsObj != null) {
            // Legacy: nested params structure
            cfg.params = ObjectConversion.convertObjectToMap(paramsObj);
        } else {
            // New: direct structure - extract parameter-related fields from input
            Map<String, Object> extractedParams = new HashMap<>();

            // Extract parameter arrays
            if (context.containsKey("parameters")) {
                extractedParams.put("parameters", context.read("parameters", Object.class));
            }
            if (context.containsKey("values")) {
                extractedParams.put("values", context.read("values", Object.class));
            }
            if (context.containsKey("jsonbParams")) {
                extractedParams.put("jsonbParams", context.read("jsonbParams", Object.class));
            }
            if (context.containsKey("arrayParams")) {
                extractedParams.put("arrayParams", context.read("arrayParams", Object.class));
            }
            if (context.containsKey("uuidParams")) {
                extractedParams.put("uuidParams", context.read("uuidParams", Object.class));
            }

            // Extract PostgreSQL-specific fields
            if (context.containsKey("schema")) {
                extractedParams.put("schema", context.read("schema", Object.class));
            }
            if (context.containsKey("conflictColumns")) {
                extractedParams.put("conflictColumns", context.read("conflictColumns", Object.class));
            }
            if (context.containsKey("updateAction")) {
                extractedParams.put("updateAction", context.read("updateAction", Object.class));
            }
            if (context.containsKey("timeout")) {
                extractedParams.put("timeout", context.read("timeout", Object.class));
            }
            if (context.containsKey("maxRows")) {
                extractedParams.put("maxRows", context.read("maxRows", Object.class));
            }
            if (context.containsKey("enableTransaction")) {
                extractedParams.put("enableTransaction", context.read("enableTransaction", Object.class));
            }

            // Handle batch query parameters
            if (context.containsKey("batchValues")) {
                extractedParams.put("batchValues", context.read("batchValues", Object.class));
            }
            if (context.containsKey("batchSize")) {
                extractedParams.put("batchSize", context.read("batchSize", Object.class));
            }

            cfg.params = extractedParams.isEmpty() ? null : extractedParams;
        }

        // Set connection ID if provided
        cfg.connectionId = (String) context.read("connectionId", String.class);

        return cfg;
    }

    public String getConnectionIdOrGenerate() {
        return connectionId != null ? connectionId : generateConnectionId();
    }

    private String generateConnectionId() {
        return String.format("%s_%s_%d_%s", driver, host, port, database);
    }

    public DbConnectionKey toConnectionKey() {
        return new DbConnectionKey(driver, host, port, database, username);
    }
}
