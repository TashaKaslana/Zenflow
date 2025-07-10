package org.phong.zenflow.log.auditlog.events;

public interface AuditLogPublishableEvent {
    CreateAuditLogPayload getAuditLog();
}
