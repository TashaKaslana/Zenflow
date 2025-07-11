package org.phong.zenflow.log.auditlog.events;

import org.phong.zenflow.log.auditlog.dtos.CreateAuditLog;

public record AuditLogEvent(CreateAuditLog log) {}
