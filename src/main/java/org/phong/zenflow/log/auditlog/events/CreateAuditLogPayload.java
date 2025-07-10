package org.phong.zenflow.log.auditlog.events;

import org.phong.zenflow.log.auditlog.enums.AuditAction;

import java.util.Map;
import java.util.UUID;

public record CreateAuditLogPayload(UUID targetId, AuditAction action, String description,
                                    Map<String, Object> metadata) {
}
