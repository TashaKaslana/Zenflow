package org.phong.zenflow.workflow.utils;

import java.util.UUID;

public class NodeKeyGenerator {

    /**
     * Generate a human-friendly key for a node.
     * Example: "http-request--a1b2c3"
     *
     * @param type Node type or plugin identifier
     * @return Readable unique key
     */
    public static String generateKey(String type) {
        String shortId = generateShortId();
        return type + "--" + shortId;
    }

    /**
     * Generate a short random ID from UUID (first 6 hex chars).
     * Can be used for readable unique suffixes.
     *
     * @return Short ID like "a1b2c3"
     */
    public static String generateShortId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 6);
    }
}