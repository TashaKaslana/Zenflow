package org.phong.zenflow.plugin.infrastructure.persistence.entity;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.phong.zenflow.core.superbase.BaseEntity;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "plugins", uniqueConstraints = {
        @UniqueConstraint(name = "plugins_name_key", columnNames = {"name"})
})
@AttributeOverrides({
        @AttributeOverride(name = "id", column = @Column(name = "id", nullable = false)),
        @AttributeOverride(name = "createdAt", column = @Column(name = "created_at", nullable = false)),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "updated_at", nullable = false))
})
public class Plugin extends BaseEntity {
    @Column(name = "publisher_id")
    private UUID publisherId;

    @NotNull
    @Column(name = "name", nullable = false, length = Integer.MAX_VALUE)
    private String name;

    @NotNull
    @Column(name = "version", nullable = false, length = Integer.MAX_VALUE)
    private String version;

    @Column(name = "registry_url", length = Integer.MAX_VALUE)
    private String registryUrl;

    @NotNull
    @ColumnDefault("false")
    @Column(name = "verified", nullable = false)
    private Boolean verified = false;

    @Column(name = "description", length = Integer.MAX_VALUE)
    private String description;

    @Column(name = "tags")
    private List<String> tags;

}