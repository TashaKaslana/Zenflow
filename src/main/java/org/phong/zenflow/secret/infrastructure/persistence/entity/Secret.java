package org.phong.zenflow.secret.infrastructure.persistence.entity;

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
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;
import org.phong.zenflow.core.superbase.BaseEntityWithUpdatedBy;
import org.phong.zenflow.project.infrastructure.persistence.entity.Project;
import org.phong.zenflow.secret.enums.SecretScope;
import org.phong.zenflow.user.infrastructure.persistence.entities.User;
import org.phong.zenflow.workflow.infrastructure.persistence.entity.Workflow;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "secrets")
@AttributeOverrides({
        @AttributeOverride(name = "id", column = @Column(name = "id", nullable = false)),
        @AttributeOverride(name = "createdAt", column = @Column(name = "created_at", nullable = false)),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "updated_at", nullable = false)),
        @AttributeOverride(name = "updatedBy", column = @Column(name = "updated_by"))
})
public class Secret extends BaseEntityWithUpdatedBy {
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "project_id")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "workflow_id")
    private Workflow workflow;

    @NotNull
    @Column(name = "key", nullable = false, length = Integer.MAX_VALUE)
    private String key;

    @NotNull
    @Column(name = "encrypted_value", nullable = false, length = Integer.MAX_VALUE)
    private String encryptedValue;

    @Column(name = "description", length = Integer.MAX_VALUE)
    private String description;

    @Column(name = "tags")
    private List<String> tags;

    @NotNull
    @ColumnDefault("1")
    @Column(name = "version")
    private Integer version = 1;

    @NotNull
    @ColumnDefault("true")
    @Column(name = "is_active")
    private Boolean isActive = true;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @NotNull
    @Column(name = "scope")
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    private SecretScope scope;

    // Decoupled from SecretProfile; association handled by link entity
}
