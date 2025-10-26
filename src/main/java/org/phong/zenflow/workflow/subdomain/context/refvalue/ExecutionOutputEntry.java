package org.phong.zenflow.workflow.subdomain.context.refvalue;

public record ExecutionOutputEntry(
    String key,
    Object value,
    WriteOptions writeOptions
) {
}