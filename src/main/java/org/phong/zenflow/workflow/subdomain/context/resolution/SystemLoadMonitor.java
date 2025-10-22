package org.phong.zenflow.workflow.subdomain.context.resolution;

import com.sun.management.OperatingSystemMXBean;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;

/**
 * Provides a lightweight view of current system load to inform caching heuristics.
 * Uses JVM MXBeans so it remains portable across supported JDKs.
 */
@Component
public class SystemLoadMonitor {

    private static final double CPU_HIGH_LOAD_THRESHOLD = 0.80d;
    private static final double MEMORY_PRESSURE_THRESHOLD = 0.85d;

    private final OperatingSystemMXBean osBean;
    private final Runtime runtime;

    public SystemLoadMonitor() {
        this.osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
        this.runtime = Runtime.getRuntime();
    }

    /**
     * Indicates whether the host is currently under a high load. We consider the system
     * heavily loaded when either CPU utilization or memory pressure crosses configured thresholds.
     */
    public boolean isHighLoad() {
        double cpuLoad = readCpuLoad();
        double memoryPressure = readMemoryPressure();
        return cpuLoad >= CPU_HIGH_LOAD_THRESHOLD || memoryPressure >= MEMORY_PRESSURE_THRESHOLD;
    }

    /**
     * Returns the current memory pressure as a value between 0.0 and 1.0.
     * Used by adaptive caching to adjust TTL under memory stress.
     */
    public double readMemoryPressure() {
        long maxMemory = runtime.maxMemory();
        if (maxMemory <= 0) {
            return 0d;
        }
        long used = runtime.totalMemory() - runtime.freeMemory();
        double pressure = (double) used / maxMemory;
        return pressure < 0d ? 0d : Math.min(pressure, 1d);
    }

    private double readCpuLoad() {
        if (osBean == null) {
            return 0d;
        }
        double load = osBean.getCpuLoad();
        return Double.isNaN(load) || load < 0d ? 0d : load;
    }
}
