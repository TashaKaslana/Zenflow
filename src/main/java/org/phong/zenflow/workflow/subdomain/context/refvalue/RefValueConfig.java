package org.phong.zenflow.workflow.subdomain.context.refvalue;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Configuration properties for RefValue storage behavior.
 * 
 * <p>Thresholds are optimized for high-throughput scenarios (millions of requests).
 * Adjust based on your heap size, expected payload distributions, and storage capacity.
 */
@Data
@Component
@ConfigurationProperties(prefix = "zenflow.context.refvalue")
public class RefValueConfig {
    
    /**
     * Directory for storing file-backed RefValues.
     * Defaults to <code>./data/context-files</code> relative to working directory.
     * 
     * <p>This directory should:
     * <ul>
     *   <li>Have sufficient disk space for large payloads</li>
     *   <li>Be on a fast disk (SSD recommended)</li>
     *   <li>Have appropriate cleanup policies (orphan detection)</li>
     * </ul>
     */
    private String fileStorageDir = "./data/context-files";
    
    /**
     * Maximum size (bytes) for memory storage before spooling to file.
     * Default: 1MB (1,048,576 bytes)
     * 
     * <p>Rationale for 1MB threshold:
     * - Million requests * 1MB = 1TB heap (unacceptable)
     * - Forces large payloads to disk early
     * - Keeps heap pressure low for GC efficiency
     */
    private long memoryThresholdBytes = 1_048_576; // 1 MB
    
    /**
     * Maximum size (bytes) for JSON tree storage before switching to file.
     * Default: 2MB (2,097,152 bytes)
     * 
     * <p>JsonRefValue keeps parsed tree in memory for fast JsonPointer queries.
     * Beyond this size, use FileRefValue with JSON media type instead.
     */
    private long jsonThresholdBytes = 2_097_152; // 2 MB
    
    /**
     * Base64 strings exceeding this decoded size will be stored as files.
     * Default: 512KB (524,288 bytes)
     * 
     * <p>Base64 encoding inflates size by ~33%, so 512KB decoded ≈ 680KB encoded.
     * Detects base64 heuristically and stores decoded binary in file.
     */
    private long base64ThresholdBytes = 524_288; // 512 KB
    
    /**
     * Enable metrics tracking for RefValue operations.
     * Tracks: ref counts by type, cleanup latency, storage size.
     */
    private boolean metricsEnabled = true;
    
    /**
     * Warn if temp files aren't cleaned up within this duration (milliseconds).
     * Default: 1 hour (3,600,000 ms)
     */
    private long fileLeakWarningMs = 3_600_000; // 1 hour
    
    /**
     * Prefix for temp file names (helps identify in filesystem).
     */
    private String tempFilePrefix = "zenflow-ctx-";
    
    /**
     * Whether to calculate checksums for file-backed values (integrity verification).
     * Adds CPU overhead but ensures data consistency.
     */
    private boolean checksumEnabled = false;
    
    /**
     * Returns the file storage directory as a Path.
     */
    public Path getFileStoragePath() {
        return Paths.get(fileStorageDir);
    }
}
