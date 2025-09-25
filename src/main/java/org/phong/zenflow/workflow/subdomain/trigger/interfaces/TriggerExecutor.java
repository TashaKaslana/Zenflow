package org.phong.zenflow.workflow.subdomain.trigger.interfaces;

import org.phong.zenflow.plugin.subdomain.execution.interfaces.PluginNodeExecutor;
import org.phong.zenflow.workflow.subdomain.trigger.dto.TriggerContext;
import org.phong.zenflow.plugin.subdomain.resource.NodeResourcePool;

import java.util.Optional;

public interface TriggerExecutor extends PluginNodeExecutor {
    RunningHandle start(TriggerContext triggerCtx, TriggerContextTool contextTool) throws Exception;

    /**
     * Optional: Return the resource manager if this trigger needs shared resources.
     * This enables the orchestrator to manage resources efficiently.
     */
    default Optional<NodeResourcePool<?, ?>> getResourceManager() {
        return Optional.empty();
    }

    /**
     * Optional: Generate the resource key for this specific trigger.
     * Only needed if this trigger uses shared resources.
     */
    default Optional<String> getResourceKey(TriggerContext trigger) {
        return Optional.empty();
    }

    interface RunningHandle {
        void stop();

        /**
         * Optional: Get the current status of this running trigger
         */
        default String getStatus() {
            return "RUNNING";
        }

        /**
         * Optional: Check if the trigger is still running/healthy
         */
        default boolean isRunning() {
            return true;
        }
    }
}
