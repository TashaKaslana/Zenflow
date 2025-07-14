package org.phong.zenflow.workflow.subdomain.trigger.infrastructure.persistence.entity;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.phong.zenflow.core.superbase.BaseFullAuditEntity;
import org.phong.zenflow.workflow.subdomain.trigger.enums.TriggerType;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "workflow_triggers", indexes = {
        @Index(name = "idx_workflow_triggers_workflow_id", columnList = "workflow_id")
})
@AttributeOverrides({
        @AttributeOverride(name = "id", column = @Column(name = "id", nullable = false)),
        @AttributeOverride(name = "createdAt", column = @Column(name = "created_at", nullable = false)),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "updated_at", nullable = false)),
        @AttributeOverride(name = "createdBy", column = @Column(name = "created_by")),
        @AttributeOverride(name = "updatedBy", column = @Column(name = "updated_by"))
})
public class WorkflowTrigger extends BaseFullAuditEntity {

    @NotNull
    @Column(name = "workflow_id", nullable = false)
    private UUID workflowId;

    @Column(name = "config")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> config;

    @NotNull
    @ColumnDefault("true")
    @Column(name = "enabled", nullable = false)
    private Boolean enabled = false;

    @Column(name = "last_triggered_at")
    private OffsetDateTime lastTriggeredAt;

    @NotNull
    @Column(name = "type", nullable = false)
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private TriggerType type;
}