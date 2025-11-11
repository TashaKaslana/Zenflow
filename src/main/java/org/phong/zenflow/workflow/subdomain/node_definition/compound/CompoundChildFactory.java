package org.phong.zenflow.workflow.subdomain.node_definition.compound;

import org.phong.zenflow.workflow.subdomain.node_definition.definitions.BaseWorkflowNode;

import java.util.List;

@FunctionalInterface
public interface CompoundChildFactory {
    List<CompoundChildDescriptor> createChildren(BaseWorkflowNode parent);
}
