package org.phong.zenflow.plugin.infrastructure.persistence.repository;

import org.phong.zenflow.plugin.infrastructure.persistence.entity.PluginNode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PluginNodeRepository extends JpaRepository<PluginNode, UUID> {
}