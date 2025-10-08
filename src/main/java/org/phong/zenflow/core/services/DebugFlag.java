package org.phong.zenflow.core.services;

import java.util.concurrent.atomic.AtomicBoolean;

public class DebugFlag {
    private static final AtomicBoolean DEBUG = new AtomicBoolean(false);

    public static boolean isDebug() {
        return DEBUG.get();
    }

    public static void enable() {
        DEBUG.set(true);
    }

    public static void disable() {
        DEBUG.set(false);
    }
}
