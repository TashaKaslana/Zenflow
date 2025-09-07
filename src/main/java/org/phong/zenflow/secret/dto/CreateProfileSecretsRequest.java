package org.phong.zenflow.secret.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateProfileSecretsRequest {
    private UUID pluginNodeId;

    @NotNull
    private String groupName;

    private List<SecretEntry> secrets;

    public boolean isDuplicated() {
        if (secrets == null || secrets.size() <= 1) {
            return false;
        }
        long uniqueCount = secrets.stream()
                .map(SecretEntry::getKey)
                .distinct()
                .count();
        return uniqueCount < secrets.size();
    }

    @Data
    public static class SecretEntry {
        @NotNull
        private String key;

        @NotNull
        private String value;

        private String description;

        private List<String> tags;
    }
}
