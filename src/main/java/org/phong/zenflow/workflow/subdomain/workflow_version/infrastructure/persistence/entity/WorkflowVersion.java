package org.phong.zenflow.workflow.subdomain.workflow_version.infrastructure.persistence.entity;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import org.hibernate.annotations.ColumnDefault;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.phong.zenflow.core.superbase.BaseIdEntity;
import org.phong.zenflow.workflow.infrastructure.persistence.entity.Workflow;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.WorkflowDefinition;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "workflow_versions",
        indexes = {@Index(name = "idx_workflow_versions_workflow_id", columnList = "workflow_id")},
        uniqueConstraints = {@UniqueConstraint(name = "uk_workflow_version", columnNames = {"workflow_id", "version"})})
@AttributeOverrides({
        @AttributeOverride(name = "id", column = @Column(name = "id", nullable = false))
})
public class WorkflowVersion extends BaseIdEntity {

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "workflow_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Workflow workflow;

    @NotNull
    @Column(name = "version", nullable = false)
    private Integer version;

    @Column(name = "definition")
    @JdbcTypeCode(SqlTypes.JSON)
    private WorkflowDefinition definition;

    @NotNull
    @ColumnDefault("false")
    @Column(name = "is_autosave", nullable = false)
    private Boolean isAutosave = false;

    @Column(name = "created_by")
    private UUID createdBy;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }
}

