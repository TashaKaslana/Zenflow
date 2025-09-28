package org.phong.zenflow.workflow.subdomain.logging.api.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;
import org.phong.zenflow.core.superbase.BaseIdEntity;
import org.phong.zenflow.workflow.subdomain.logging.api.enums.LogLevel;
import org.phong.zenflow.workflow.subdomain.workflow_run.infrastructure.persistence.entity.WorkflowRun;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "node_logs", indexes = {
        @Index(name = "idx_node_log_workflow_run_id", columnList = "workflow_run_id"),
        @Index(name = "idx_node_log_node_key", columnList = "node_key")
})
public class NodeLog extends BaseIdEntity {
    @NotNull
    @Column(name = "workflow_id", nullable = false)
    private UUID workflowId;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "workflow_run_id", nullable = false)
    private WorkflowRun workflowRun;

    @NotNull
    @Column(name = "node_key", nullable = false, length = Integer.MAX_VALUE)
    private String nodeKey;

    @NotNull
    @Column(name = "timestamp", nullable = false)
    private OffsetDateTime timestamp;

    @NotNull
    @Column(name = "level", nullable = false, length = Integer.MAX_VALUE)
    @Enumerated(EnumType.STRING)
    private LogLevel level;

    @Column(name = "message", length = Integer.MAX_VALUE)
    private String message;

    @Column(name = "error_code", length = Integer.MAX_VALUE)
    private String errorCode;

    @Column(name = "error_message", length = Integer.MAX_VALUE)
    private String errorMessage;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "meta")
    private Map<String, Object> meta;

    @Column(name = "trace_id", length = Integer.MAX_VALUE)
    private String traceId;

    @Column(name = "hierarchy", length = Integer.MAX_VALUE)
    private String hierarchy;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "correlation_id", length = Integer.MAX_VALUE)
    private String correlationId;
}
