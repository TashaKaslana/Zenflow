package org.phong.zenflow.workflow.subdomain.context;

import org.phong.zenflow.workflow.subdomain.context.refvalue.StoragePreference;
import org.springframework.lang.Nullable;

/**
 * Options for context.write() to control storage behavior.
 * Default: write(key, value) uses DEFAULT options (auto storage, auto cleanup).
 * Explicit: write(key, value, WriteOptions.base64()) for special cases.
 */
public record WriteOptions(
    @Nullable String mediaType,
    @Nullable StoragePreference storage,
    boolean autoCleanup
) {
    public static final WriteOptions DEFAULT = new WriteOptions(null, StoragePreference.AUTO, true);
    
    public static WriteOptions base64() {
        return new WriteOptions("text/base64", StoragePreference.AUTO, true);
    }
    
    public static WriteOptions forceFile() {
        return new WriteOptions(null, StoragePreference.FILE, true);
    }
    
    public static WriteOptions persistent() {
        return new WriteOptions(null, StoragePreference.AUTO, false);
    }
    
    public static WriteOptions withMediaType(String mediaType) {
        return new WriteOptions(mediaType, StoragePreference.AUTO, true);
    }
}
