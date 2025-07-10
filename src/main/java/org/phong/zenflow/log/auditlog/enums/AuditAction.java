package org.phong.zenflow.log.auditlog.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum AuditAction {
    WORKFLOW_DELETE("workflow.delete", TargetType.WORKFLOW, "Delete workflow");

    final String action;
    final TargetType targetType;
    final String description;
}
