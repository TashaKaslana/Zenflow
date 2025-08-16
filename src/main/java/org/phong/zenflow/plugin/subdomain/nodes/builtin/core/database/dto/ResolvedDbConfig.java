package org.phong.zenflow.plugin.subdomain.nodes.builtin.core.database.dto;

import lombok.Data;
import org.phong.zenflow.core.utils.ObjectConversion;

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

    public static ResolvedDbConfig fromInput(Map<String, Object> input) {
        ResolvedDbConfig cfg = new ResolvedDbConfig();
        cfg.driver = (String) input.get("driver"); // e.g. "postgresql"
        cfg.host = (String) input.get("host");
        cfg.port = (int) input.get("port");
        cfg.database = (String) input.get("database");
        cfg.username = (String) input.get("username");
        cfg.password = (String) input.get("password");
        cfg.query = (String) input.get("query");
        cfg.returnType = (String) input.get("returnType");

        // Handle params extraction - support both nested and direct structure
        Object paramsObj = input.get("params");
        if (paramsObj != null) {
            // Legacy: nested params structure
            cfg.params = ObjectConversion.convertObjectToMap(paramsObj);
        } else {
            // New: direct structure - extract parameter-related fields from input
            Map<String, Object> extractedParams = new HashMap<>();

            // Extract parameter arrays
            if (input.containsKey("parameters")) {
                extractedParams.put("parameters", input.get("parameters"));
            }
            if (input.containsKey("values")) {
                extractedParams.put("values", input.get("values"));
            }
            if (input.containsKey("jsonbParams")) {
                extractedParams.put("jsonbParams", input.get("jsonbParams"));
            }
            if (input.containsKey("arrayParams")) {
                extractedParams.put("arrayParams", input.get("arrayParams"));
            }
            if (input.containsKey("uuidParams")) {
                extractedParams.put("uuidParams", input.get("uuidParams"));
            }

            // Extract PostgreSQL-specific fields
            if (input.containsKey("schema")) {
                extractedParams.put("schema", input.get("schema"));
            }
            if (input.containsKey("conflictColumns")) {
                extractedParams.put("conflictColumns", input.get("conflictColumns"));
            }
            if (input.containsKey("updateAction")) {
                extractedParams.put("updateAction", input.get("updateAction"));
            }
            if (input.containsKey("timeout")) {
                extractedParams.put("timeout", input.get("timeout"));
            }
            if (input.containsKey("maxRows")) {
                extractedParams.put("maxRows", input.get("maxRows"));
            }
            if (input.containsKey("enableTransaction")) {
                extractedParams.put("enableTransaction", input.get("enableTransaction"));
            }

            // Handle batch query parameters
            if (input.containsKey("batchValues")) {
                extractedParams.put("batchValues", input.get("batchValues"));
            }
            if (input.containsKey("batchSize")) {
                extractedParams.put("batchSize", input.get("batchSize"));
            }

            cfg.params = extractedParams.isEmpty() ? null : extractedParams;
        }

        // Set connection ID if provided
        cfg.connectionId = (String) input.get("connectionId");

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
