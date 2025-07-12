package org.phong.zenflow.secret.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SecretScope {
    GLOBAL("global"),
    PROJECT("project"),
    WORKFLOW("workflow");

    private final String scope;
}
