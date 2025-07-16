package org.phong.zenflow.workflow.subdomain.node_logs.infraustructure.persistence.entity;

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
import org.phong.zenflow.core.superbase.BaseIdEntity;
import org.phong.zenflow.workflow.subdomain.workflow_run.infrastructure.persistence.entity.WorkflowRun;

import java.time.OffsetDateTime;
import java.util.Map;

@Getter
@Setter
@Entity
@Table(name = "node_logs", indexes = {
        @Index(name = "idx_node_logs_workflow_run_id", columnList = "workflow_run_id")
})
public class NodeLog extends BaseIdEntity {
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "workflow_run_id", nullable = false)
    private WorkflowRun workflowRun;

    @NotNull
    @Column(name = "node_key", nullable = false, length = Integer.MAX_VALUE)
    private String nodeKey;

    @NotNull
    @Column(name = "status", nullable = false, length = Integer.MAX_VALUE)
    private String status;

    @Column(name = "error", length = Integer.MAX_VALUE)
    private String error;

    @ColumnDefault("1")
    @Column(name = "attempts")
    private Integer attempts;

    @Column(name = "output")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> output;

    @NotNull
    @ColumnDefault("now()")
    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt;

    @Column(name = "ended_at")
    private OffsetDateTime endedAt;

    @Column(name = "logs")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> logs;
}