package org.phong.zenflow.plugin.subdomain.node.infrastructure.persistence.entity;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;
import org.phong.zenflow.core.superbase.BaseEntity;
import org.phong.zenflow.plugin.infrastructure.persistence.entity.Plugin;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@Entity
@Table(name = "plugin_nodes", indexes = {
        @Index(name = "idx_plugin_nodes_plugin_id", columnList = "plugin_id")
})
@AttributeOverrides({
        @AttributeOverride(name = "id", column = @Column(name = "id", nullable = false)),
        @AttributeOverride(name = "createdAt", column = @Column(name = "created_at", nullable = false)),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "updated_at", nullable = false))
})
public class PluginNode extends BaseEntity {
    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "plugin_id", nullable = false)
    private Plugin plugin;

    @NotNull
    @Column(name = "name", nullable = false, length = Integer.MAX_VALUE)
    private String name;

    @NotNull
    @Column(name = "type", nullable = false, length = Integer.MAX_VALUE)
    private String type;

    @Column(name = "plugin_node_version", length = Integer.MAX_VALUE)
    private String pluginNodeVersion;

    @Column(name = "config_schema")
    @JdbcTypeCode(SqlTypes.JSON)
    private Map<String, Object> configSchema;

    @NotNull
    @Column(name = "executor_type", nullable = false, length = 50)
    private String executorType;

    @Column(name = "entrypoint", length = Integer.MAX_VALUE)
    private String entrypoint;

    @Column(name = "description", length = Integer.MAX_VALUE)
    private String description;

    @Column(name = "tags")
    private List<String> tags;

    @Column(name = "icon", length = Integer.MAX_VALUE)
    private String icon;

    @Column(name = "key", length = Integer.MAX_VALUE)
    private String key;
}