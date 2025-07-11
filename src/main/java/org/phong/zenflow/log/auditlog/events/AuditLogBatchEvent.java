package org.phong.zenflow.log.auditlog.events;

import org.phong.zenflow.log.auditlog.dtos.CreateAuditLog;

import java.util.List;

public record AuditLogBatchEvent(List<CreateAuditLog> logs) {}