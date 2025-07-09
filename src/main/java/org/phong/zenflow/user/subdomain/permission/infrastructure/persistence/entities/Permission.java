package org.phong.zenflow.user.subdomain.permission.infrastructure.persistence.entities;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.phong.zenflow.core.superbase.BaseIdEntity;

@Getter
@Setter
@Entity
@Table(name = "permissions", uniqueConstraints = {
        @UniqueConstraint(name = "permissions_feature_action_key", columnNames = {"feature", "action"})
})
@AttributeOverrides({
        @AttributeOverride(name = "id", column = @Column(name = "id", nullable = false))
})
public class Permission extends BaseIdEntity {
    @NotNull
    @Column(name = "feature", nullable = false, length = Integer.MAX_VALUE)
    private String feature;

    @NotNull
    @Column(name = "action", nullable = false, length = Integer.MAX_VALUE)
    private String action;

    @Column(name = "description", length = Integer.MAX_VALUE)
    private String description;

}