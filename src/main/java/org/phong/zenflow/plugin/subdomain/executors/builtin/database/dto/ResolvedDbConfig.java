package org.phong.zenflow.plugin.subdomain.executors.builtin.database.dto;

import lombok.Data;
import org.phong.zenflow.core.utils.ObjectConversion;

import javax.sql.DataSource;
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
        cfg.params = ObjectConversion.convertObjectToMap(input.get("params"));

        cfg.connectionId = (String) input.get("connectionId");
        return cfg;
    }

    public String getConnectionIdOrGenerate() {
        return connectionId != null ? connectionId : String.format("%s-%s-%d-%s-%s",
            driver, host, port, database, username);
    }

    public DbConnectionKey toConnectionKey() {
        return new DbConnectionKey(driver, host, port, database, username);
    }
}
