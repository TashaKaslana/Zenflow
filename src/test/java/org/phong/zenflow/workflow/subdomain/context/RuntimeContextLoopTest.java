package org.phong.zenflow.workflow.subdomain.context;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RuntimeContextLoopTest {

    @Test
    void valuePersistsUntilLoopEnds() {
        RuntimeContext ctx = new RuntimeContext();
        ctx.initialize(Map.of("foo", "bar"), Map.of("foo", Set.of("node")), Map.of());

        ctx.startLoop("loop");

        assertEquals("bar", ctx.getAndClean("node", "foo"));
        // value should remain accessible during loop
        assertEquals("bar", ctx.getAndClean("node", "foo"));
        assertEquals("bar", ctx.get("foo"));

        ctx.endLoop("loop");
        assertNull(ctx.get("foo"));
    }
}

