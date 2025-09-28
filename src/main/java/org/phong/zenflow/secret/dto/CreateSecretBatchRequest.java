package org.phong.zenflow.secret.dto;

import java.io.Serializable;
import java.util.List;

public record CreateSecretBatchRequest(List<CreateSecretRequest> secrets) implements Serializable {
}
