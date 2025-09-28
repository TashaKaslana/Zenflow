package org.phong.zenflow.secret.subdomain.profile.dto;

import java.io.Serializable;
import java.util.Map;

public record ProfileSecretListDto(Map<String, Map<String, String>> profiles) implements Serializable {
}
