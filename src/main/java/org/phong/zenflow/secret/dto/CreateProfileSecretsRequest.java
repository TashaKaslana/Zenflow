package org.phong.zenflow.secret.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateProfileSecretsRequest {
    private UUID pluginId;

    private UUID pluginNodeId;

    @NotNull
    private String name;

    /**
     * Secrets represented as key:value pairs
     * Example:
     * {
     *   "CLIENT_ID": "123456",
     *   "CLIENT_SECRET": "secret"
     * }
     */
    @NotNull
    private Map<String, String> secrets;

    public boolean isDuplicated() {
        if (secrets == null) {
            return false;
        }
        long uniqueCount = secrets.keySet().stream().distinct().count();
        return uniqueCount < secrets.size();
    }
}
