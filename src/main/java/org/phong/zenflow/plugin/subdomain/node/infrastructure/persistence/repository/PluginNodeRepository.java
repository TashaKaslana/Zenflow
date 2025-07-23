package org.phong.zenflow.plugin.subdomain.node.infrastructure.persistence.repository;

import org.phong.zenflow.plugin.subdomain.node.infrastructure.persistence.entity.PluginNode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PluginNodeRepository extends JpaRepository<PluginNode, UUID> {
    Optional<PluginNode> findByName(String name);

    Page<PluginNode> findAllByPluginId(UUID pluginId, Pageable pageable);

    Optional<PluginNode> findByPluginIdAndId(UUID pluginId, UUID id);
}