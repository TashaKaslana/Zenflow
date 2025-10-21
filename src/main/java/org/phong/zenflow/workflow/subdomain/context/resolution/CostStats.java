package org.phong.zenflow.workflow.subdomain.context.resolution;

final class CostStats {
    private static final double ALPHA = 0.2; // smoothing
    private volatile double emaWallMs = 0.0;

    void record(long nanos) {
        double ms = nanos / 1_000_000.0;
        double prev = emaWallMs;
        emaWallMs = (prev == 0.0) ? ms : (ALPHA * ms + (1 - ALPHA) * prev);
    }

    double wallMs() { return emaWallMs; }
}