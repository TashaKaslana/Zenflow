package org.phong.zenflow.log.auditlog.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AuditAction {

    // ==== USER ====
    USER_LOGIN("user.login", TargetType.USER),
    USER_LOGOUT("user.logout", TargetType.USER),
    USER_CREATE("user.create", TargetType.USER),
    USER_UPDATE("user.update", TargetType.USER),
    USER_DELETE("user.delete", TargetType.USER),

    // ==== SECRET ====
    SECRET_CREATE("secret.create", TargetType.SECRET),
    SECRET_UPDATE("secret.update", TargetType.SECRET),
    SECRET_DELETE("secret.delete", TargetType.SECRET),

    // ==== WORKFLOW ====
    WORKFLOW_CREATE("workflow.create", TargetType.WORKFLOW),
    WORKFLOW_UPDATE("workflow.update", TargetType.WORKFLOW),
    WORKFLOW_DELETE("workflow.delete", TargetType.WORKFLOW),
    WORKFLOW_EXECUTE("workflow.execute", TargetType.WORKFLOW),
    WORKFLOW_VERSION_RESTORE("workflow.version.restore", TargetType.WORKFLOW),

    // ==== PROJECT ====
    PROJECT_CREATE("project.create", TargetType.PROJECT),
    PROJECT_UPDATE("project.update", TargetType.PROJECT),
    PROJECT_DELETE("project.delete", TargetType.PROJECT),

    // ==== PLUGIN ====
    PLUGIN_UPLOAD("plugin.upload", TargetType.PLUGIN),
    PLUGIN_UPDATE("plugin.update", TargetType.PLUGIN),
    PLUGIN_DELETE("plugin.delete", TargetType.PLUGIN),
    PLUGIN_NODE_CREATE("plugin.node.create", TargetType.PLUGIN),
    PLUGIN_NODE_UPDATE("plugin.node.update", TargetType.PLUGIN),
    PLUGIN_NODE_DELETE("plugin.node.delete", TargetType.PLUGIN),

    // ==== SETTING ====
    SETTING_UPDATE("setting.update", TargetType.SETTING),

    // ==== ROLE ====
    ROLE_CREATE("role.create", TargetType.ROLE),
    ROLE_UPDATE("role.update", TargetType.ROLE),
    ROLE_DELETE("role.delete", TargetType.ROLE),

    // ==== PERMISSION ====
    PERMISSION_CREATE("permission.create", TargetType.PERMISSION),
    PERMISSION_UPDATE("permission.update", TargetType.PERMISSION),
    PERMISSION_DELETE("permission.delete", TargetType.PERMISSION),

    // ==== DEFAULT / OTHER ====
    SYSTEM_CONFIG_UPDATE("system.config.update", TargetType.OTHER),
    UNKNOWN("unknown", TargetType.OTHER);

    private final String action;
    private final TargetType targetType;
}

