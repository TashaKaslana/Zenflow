package org.phong.zenflow.plugin.subdomain.node.infrastructure.persistence.repository;

import org.phong.zenflow.plugin.subdomain.node.infrastructure.persistence.entity.PluginNode;
import org.phong.zenflow.plugin.subdomain.node.infrastructure.persistence.projections.PluginNodeId;
import org.phong.zenflow.plugin.subdomain.node.infrastructure.persistence.projections.PluginNodeSchema;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;


import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface PluginNodeRepository extends JpaRepository<PluginNode, UUID>, JpaSpecificationExecutor<PluginNode> {
    Optional<PluginNode> findByName(String name);

    Page<PluginNode> findAllByPluginId(UUID pluginId, Pageable pageable);

    Optional<PluginNode> findByCompositeKey(String key);

    @Query(
            "SELECT CAST(p.id AS string) as id, p.configSchema as configSchema from PluginNode p where p.id in :nodeIds"
    )
    Set<PluginNodeSchema> findAllSchemasByNodeIds(Set<UUID> nodeIds);

    @Query(
            "SELECT CAST(p.id AS string) as id, p.compositeKey as compositeKey FROM PluginNode p WHERE p.compositeKey IN :compositeKeys"
    )
    Set<PluginNodeId> findIdsByCompositeKeys(Set<String> compositeKeys);
}