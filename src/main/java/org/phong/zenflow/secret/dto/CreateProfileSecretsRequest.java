package org.phong.zenflow.secret.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateProfileSecretsRequest {
    @NotNull
    private String groupName;

    private List<SecretEntry> secrets;

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
