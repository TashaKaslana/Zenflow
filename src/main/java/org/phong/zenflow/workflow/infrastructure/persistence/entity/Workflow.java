package org.phong.zenflow.workflow.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;
import org.phong.zenflow.core.superbase.BaseFullAuditEntity;
import org.phong.zenflow.project.infrastructure.persistence.entity.Project;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.WorkflowDefinition;

import java.time.OffsetDateTime;
import java.util.Map;

@Getter
@Setter
@Entity
@Table(name = "workflows", indexes = {
        @Index(name = "idx_workflows_project_id", columnList = "project_id")
})
public class Workflow extends BaseFullAuditEntity {
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @NotNull
    @Column(name = "name", nullable = false, length = Integer.MAX_VALUE)
    private String name;

    @Column(name = "definition")
    @JdbcTypeCode(SqlTypes.JSON)
    private WorkflowDefinition definition;

    @Column(name = "start_node", length = Integer.MAX_VALUE)
    private String startNode;

    @NotNull
    @ColumnDefault("false")
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = false;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @Column(name = "retry_policy")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> retryPolicy;

    @Column(name = "description", length = Integer.MAX_VALUE)
    private String description;
}