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
import org.phong.zenflow.secret.subdomain.profile.entity.SecretProfile;

@Getter
@Setter
@Entity
@Table(name = "profile_secret_links", uniqueConstraints = {
        @UniqueConstraint(name = "profile_secret_links_profile_secret_unique", columnNames = {"profile_id", "secret_id"})
})
@AttributeOverrides({
        @AttributeOverride(name = "id", column = @Column(name = "id", nullable = false))
})
public class ProfileSecretLink extends BaseIdEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "profile_id", nullable = false)
    private SecretProfile profile;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "secret_id", nullable = false)
    private Secret secret;
}

