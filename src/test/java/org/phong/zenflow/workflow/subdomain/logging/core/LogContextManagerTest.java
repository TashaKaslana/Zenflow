package org.phong.zenflow.workflow.subdomain.logging.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class LogContextManagerTest {

    @AfterEach
    void tearDown() {
        LogContextManager.cleanupAll();
    }

    @Test
    void snapshotReturnsRootFirstOrder() {
        LogContextManager.init("ctx", "trace");
        LogContextManager.push("root");
        LogContextManager.push("child");

        LogContext snapshot = LogContextManager.snapshot();
        assertEquals("root->child", snapshot.hierarchy());
    }
}
