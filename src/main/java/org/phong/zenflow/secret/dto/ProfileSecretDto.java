package org.phong.zenflow.secret.dto;

import java.io.Serializable;
import java.util.Map;

public record ProfileSecretDto(Map<String, Map<String, String>> profiles) implements Serializable {

}
