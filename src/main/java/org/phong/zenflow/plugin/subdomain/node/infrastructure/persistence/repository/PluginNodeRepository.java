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

    @Query("SELECT pns.key AS key, pns.configSchema AS configSchema " +
            "FROM PluginNode pns WHERE pns.key IN :keyList")
    List<PluginNodeSchema> findAllByKeyList(List<String> keyList);

    Optional<PluginNode> findByKey(String key);
}