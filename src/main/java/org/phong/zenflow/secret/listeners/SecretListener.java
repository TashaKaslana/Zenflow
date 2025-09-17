package org.phong.zenflow.secret.listeners;

import lombok.AllArgsConstructor;
import org.phong.zenflow.secret.service.SecretLinkSyncService;
import org.phong.zenflow.workflow.service.event.WorkflowDefinitionUpdatedEvent;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@AllArgsConstructor
public class SecretListener {
    private final SecretLinkSyncService linkSyncService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onWorkflowDefinitionUpdated(WorkflowDefinitionUpdatedEvent event) {
        linkSyncService.syncLinksFromMetadata(event.workflowId(), event.definition());
    }
}
