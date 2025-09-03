package org.phong.zenflow.plugin.subdomain.node.infrastructure.persistence.repository;

import jakarta.persistence.criteria.Predicate;
import org.phong.zenflow.plugin.subdomain.node.infrastructure.persistence.entity.PluginNode;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.UUID;

public class PluginNodeSpecifications {
    public static Specification<PluginNode> withIds(List<String> nodeIds) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = nodeIds.stream()
                    .map(nodeId -> criteriaBuilder.equal(root.get("id"), UUID.fromString(nodeId)))
                    .toList();
            return criteriaBuilder.or(predicates.toArray(new Predicate[0]));
        };
    }
}