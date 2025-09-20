package org.phong.zenflow.secret.subdomain.link.infrastructure.entity;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.phong.zenflow.core.superbase.BaseIdEntity;
import org.phong.zenflow.secret.infrastructure.persistence.entity.Secret;
import org.phong.zenflow.workflow.infrastructure.persistence.entity.Workflow;

@Getter
@Setter
@Entity
@Table(name = "secret_node_links", uniqueConstraints = {
        @UniqueConstraint(name = "secret_node_links_workflow_node_secret_unique", columnNames = {"workflow_id", "node_key", "secret_id"})
})
@AttributeOverrides({
        @AttributeOverride(name = "id", column = @Column(name = "id", nullable = false))
})
public class SecretNodeLink extends BaseIdEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "workflow_id", nullable = false)
    private Workflow workflow;

    @Column(name = "node_key", nullable = false)
    private String nodeKey;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "secret_id", nullable = false)
    private Secret secret;
}

