package org.phong.zenflow.plugin.subdomain.nodes.builtin.integration.database.base.dto;

import lombok.Data;

import java.util.Objects;

@Data
public class DbConnectionKey {
    private final String driver;     // e.g., "postgresql"
    private final String host;
    private final int port;
    private final String database;
    private final String username;

    public DbConnectionKey(String driver, String host, int port, String database, String username) {
        this.driver = driver;
        this.host = host;
        this.port = port;
        this.database = database;
        this.username = username;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DbConnectionKey that)) return false;
        return port == that.port &&
                Objects.equals(driver, that.driver) &&
                Objects.equals(host, that.host) &&
                Objects.equals(database, that.database) &&
                Objects.equals(username, that.username);
    }

    @Override
    public int hashCode() {
        return Objects.hash(driver, host, port, database, username);
    }

    @Override
    public String toString() {
        return driver + "://" + username + "@" + host + ":" + port + "/" + database;
    }

    public String getDatabaseSource() {
        return String.format("%s://%s:%d/%s", driver, host, port, database);
    }
}
