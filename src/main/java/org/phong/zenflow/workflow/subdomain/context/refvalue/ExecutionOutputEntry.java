package org.phong.zenflow.workflow.subdomain.context.refvalue;

import org.phong.zenflow.workflow.subdomain.context.refvalue.dto.WriteOptions;

public record ExecutionOutputEntry(
    String key,
    Object value,
    WriteOptions writeOptions
) {
}