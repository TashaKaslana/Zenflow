package org.phong.zenflow.secret.subdomain.link.dto;

import java.io.Serializable;
import java.util.UUID;

public record SecretNodeLinkInsertRequestDto(UUID secretId, String nodeKey) implements Serializable {

}
