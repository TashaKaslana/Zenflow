package org.phong.zenflow.plugin.subdomain.node.definition;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.phong.zenflow.plugin.subdomain.node.definition.aspect.NodeExecutor;
import org.phong.zenflow.plugin.subdomain.node.definition.aspect.NodeValidator;
import org.phong.zenflow.plugin.subdomain.node.definition.policy.NodeExecutionPolicy;
import org.phong.zenflow.plugin.subdomain.resource.BaseNodeResourceManager;
import org.phong.zenflow.workflow.subdomain.trigger.interfaces.TriggerExecutor;
import org.springframework.lang.Nullable;

import java.util.Objects;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
public class NodeDefinition {
    NodeExecutor nodeExecutor;

    @Nullable
    BaseNodeResourceManager<?, ?> nodeResourceManager;

    @Nullable
    NodeValidator nodeValidator;

    @Nullable
    NodeExecutionPolicy executionPolicy;

    String name;
    String description;
    String icon;
    String type;
    String[] tags;

    @Nullable
    Boolean autoAcquireResource;

    public boolean shouldAutoAcquireResource() {
        if (nodeResourceManager == null) {
            return false;
        }

        return Objects.requireNonNullElseGet(
                autoAcquireResource,
                () -> !(nodeExecutor instanceof TriggerExecutor)
        );
    }
}
