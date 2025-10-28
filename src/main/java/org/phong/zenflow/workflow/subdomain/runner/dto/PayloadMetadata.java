package org.phong.zenflow.workflow.subdomain.runner.dto;

import org.phong.zenflow.workflow.subdomain.context.refvalue.dto.StoragePreference;
import org.springframework.lang.Nullable;

/**
 * Metadata hints for payload values to control RefValue storage behavior.
 * 
 * <p>Used when client needs to specify how a payload value should be stored:
 * <ul>
 *   <li>Base64-encoded files (mediaType = "text/base64")</li>
 *   <li>Force large files to use file storage</li>
 *   <li>Override auto-detection heuristics</li>
 * </ul>
 * 
 * <p><b>Example usage:</b>
 * <pre>
 * {
 *   "payload": {
 *     "image": "iVBORw0KGgoAAAANS...",
 *     "description": "A photo"
 *   },
 *   "payloadMetadata": {
 *     "image": {
 *       "mediaType": "text/base64",
 *       "storagePreference": "AUTO"
 *     }
 *   }
 * }
 * </pre>
 * 
 * @param mediaType MIME type hint (e.g., "text/base64", "application/json", "video/mp4")
 * @param storagePreference Storage backend preference (AUTO, MEMORY, JSON, FILE)
 */
public record PayloadMetadata(
        @Nullable String mediaType,
        @Nullable StoragePreference storagePreference
) {
    /**
     * Creates metadata for base64-encoded data.
     */
    public static PayloadMetadata base64() {
        return new PayloadMetadata("text/base64", StoragePreference.AUTO);
    }
    
    /**
     * Creates metadata forcing file storage (useful for large files).
     */
    public static PayloadMetadata forceFile() {
        return new PayloadMetadata(null, StoragePreference.FILE);
    }
    
    /**
     * Creates metadata with just media type.
     */
    public static PayloadMetadata withMediaType(String mediaType) {
        return new PayloadMetadata(mediaType, StoragePreference.AUTO);
    }
}
