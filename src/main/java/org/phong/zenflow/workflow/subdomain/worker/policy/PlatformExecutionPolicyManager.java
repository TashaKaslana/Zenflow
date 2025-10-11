package org.phong.zenflow.workflow.subdomain.worker.policy;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

@Component
@RequiredArgsConstructor
public class PlatformExecutionPolicyManager {

    private final PlatformExecutionPolicyProperties properties;

    public PlatformExecutionPolicyProperties properties() {
        return properties;
    }

    public synchronized void update(Consumer<PlatformExecutionPolicyProperties> updater) {
        if (updater != null) {
            updater.accept(properties);
        }
    }
}
