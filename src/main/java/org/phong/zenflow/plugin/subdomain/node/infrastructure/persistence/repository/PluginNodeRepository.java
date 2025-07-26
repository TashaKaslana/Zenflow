package org.phong.zenflow.plugin.subdomain.node.infrastructure.persistence.repository;

import org.phong.zenflow.plugin.subdomain.node.infrastructure.persistence.entity.PluginNode;
import org.phong.zenflow.plugin.subdomain.node.infrastructure.persistence.projections.PluginNodeSchema;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PluginNodeRepository extends JpaRepository<PluginNode, UUID> {
    Optional<PluginNode> findByName(String name);

    Page<PluginNode> findAllByPluginId(UUID pluginId, Pageable pageable);

    @Query("SELECT pns.id AS id, pns.configSchema AS configSchema " +
            "FROM PluginNode pns WHERE pns.id = :id")
    Optional<PluginNodeSchema> findByIdCustom(UUID id);

    @Query("SELECT pns.id AS id, pns.configSchema AS configSchema " +
            "FROM PluginNode pns WHERE pns.id IN :nodeIds")
    List<PluginNodeSchema> findAllByIds(List<UUID> nodeIds);
}