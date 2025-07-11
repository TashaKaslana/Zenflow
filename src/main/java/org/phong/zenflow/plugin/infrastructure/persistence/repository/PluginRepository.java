package org.phong.zenflow.plugin.infrastructure.persistence.repository;

import org.phong.zenflow.plugin.infrastructure.persistence.entity.Plugin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PluginRepository extends JpaRepository<Plugin, UUID> {
    Optional<Plugin> findByName(String name);
}