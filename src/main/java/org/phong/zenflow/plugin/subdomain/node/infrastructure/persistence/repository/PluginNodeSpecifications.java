package org.phong.zenflow.plugin.subdomain.node.infrastructure.persistence.repository;

import jakarta.persistence.criteria.Predicate;
import org.phong.zenflow.plugin.subdomain.node.infrastructure.persistence.entity.PluginNode;
import org.phong.zenflow.workflow.subdomain.node_definition.definitions.plugin.PluginNodeIdentifier;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public class PluginNodeSpecifications {

    public static Specification<PluginNode> withIdentifiers(List<PluginNodeIdentifier> identifiers) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = identifiers.stream()
                    .map(identifier -> criteriaBuilder.and(
                            criteriaBuilder.equal(root.get("key"), identifier.nodeKey()),
                            criteriaBuilder.equal(root.get("plugin").get("key"), identifier.pluginKey()),
                            criteriaBuilder.equal(root.get("pluginNodeVersion"), identifier.version())
                    ))
                    .toList();
            return criteriaBuilder.or(predicates.toArray(new Predicate[0]));
        };
    }
}