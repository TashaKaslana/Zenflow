package org.phong.zenflow.secret.subdomain.aggregate;

import java.io.Serializable;
import java.util.Map;

public record AggregatedSecretDto(Map<String, String> secrets) implements Serializable {
}
