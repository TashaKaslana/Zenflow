package org.phong.zenflow.secret.subdomain.profile.entity;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.phong.zenflow.core.superbase.BaseEntity;
import org.phong.zenflow.plugin.infrastructure.persistence.entity.Plugin;
import org.phong.zenflow.plugin.subdomain.node.infrastructure.persistence.entity.PluginNode;
import org.phong.zenflow.project.infrastructure.persistence.entity.Project;
import org.phong.zenflow.secret.enums.SecretScope;
import org.phong.zenflow.user.infrastructure.persistence.entities.User;
import org.phong.zenflow.workflow.infrastructure.persistence.entity.Workflow;

@Getter
@Setter
@Entity
@Table(name = "secret_profiles", uniqueConstraints = {
        @UniqueConstraint(name = "secret_profiles_scope_project_workflow_plugin_name_key",
                columnNames = {"scope", "project_id", "workflow_id", "plugin_id", "name"})
})
@AttributeOverrides({
        @AttributeOverride(name = "id", column = @Column(name = "id", nullable = false)),
})
public class SecretProfile extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workflow_id")
    private Workflow workflow;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plugin_id", nullable = false)
    private Plugin plugin;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "plugin_node_id")
    private PluginNode pluginNode;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "scope", nullable = false)
    private SecretScope scope;
}
