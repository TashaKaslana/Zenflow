package org.phong.zenflow.workflow.subdomain.node_logs.logging.durable;// logging/LoggingConfig.java
import javax.sql.DataSource;

public class LoggingConfig {
    public static void wire(DataSource ds, WebSocketNotifier ws){
        // Durable sinks
        PersistenceService persistence = new JdbcPersistenceService(ds);
        KafkaPublisher kafka = null; // plug later

        // Global collector
        GlobalLogCollector collector = new GlobalLogCollector(persistence, kafka, 2);

        // Buffer manager
        WorkflowBufferManager bufferMgr = new WorkflowBufferManager(collector,
                /*batchSize*/100, /*maxDelayMs*/2000, /*ringSize*/200);

        // Router (workers=2)
        LogRouter.init(ws, bufferMgr, null, 2);
    }
}
