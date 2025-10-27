package org.phong.zenflow.workflow.subdomain.context.refvalue.impl;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.phong.zenflow.core.utils.ObjectConversion;
import org.phong.zenflow.workflow.subdomain.context.refvalue.*;
import org.phong.zenflow.workflow.subdomain.context.refvalue.dto.RefValueType;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * File-backed storage for large payloads.
 * Stores content in a temporary file and provides streaming access.
 * 
 * <p>Files are automatically deleted when onRelease() is called (consumer count reaches zero).
 * Uses a custom directory configured via application properties to avoid OS temp cleanup issues.
 * 
 * <p>Use cases:
 * <ul>
 *   <li>Large JSON documents (> 3MB)</li>
 *   <li>Binary files (videos, images, archives)</li>
 *   <li>Base64-decoded payloads</li>
 *   <li>Streaming data transfers</li>
 * </ul>
 * 
 * <p><b>Thread Safety:</b> onRelease() is idempotent and can be called multiple times.
 */
@Slf4j
public class FileRefValue implements RefValue {

    /**
     *  Gets the file path (for advanced use cases).
     *  Caller must ensure the file hasn't been released.
     */
    @Getter
    private final Path filePath;
    private final String mediaType;
    private final long size;
    private final AtomicBoolean released = new AtomicBoolean(false);
    
    /**
     * Creates a FileRefValue from an existing file.
     * The file will be managed by this RefValue and deleted on release.
     * 
     * @param filePath path to the file
     * @param mediaType content type (e.g., "application/json", "video/mp4")
     * @throws IOException if file doesn't exist or cannot be read
     */
    public FileRefValue(Path filePath, String mediaType) throws IOException {
        if (!Files.exists(filePath)) {
            throw new IOException("File does not exist: " + filePath);
        }
        this.filePath = filePath;
        this.mediaType = mediaType;
        this.size = Files.size(filePath);
        log.debug("Created FileRefValue: {} (size: {} bytes, type: {})", 
                filePath.getFileName(), size, mediaType);
    }
    
    /**
     * Creates a FileRefValue by writing an input stream to a temp file.
     * 
     * @param stream input stream to read from
     * @param mediaType content type
     * @param targetDir directory to store temp files
     * @param prefix filename prefix
     * @return new FileRefValue
     * @throws IOException if writing fails
     */
    public static FileRefValue fromStream(InputStream stream, String mediaType, 
                                         Path targetDir, String prefix) throws IOException {
        Files.createDirectories(targetDir);
        String suffix = mediaType != null ? guessSuffix(mediaType) : ".dat";
        Path tempFile = Files.createTempFile(targetDir, prefix, suffix);
        
        try {
            Files.copy(stream, tempFile, StandardCopyOption.REPLACE_EXISTING);
            return new FileRefValue(tempFile, mediaType);
        } catch (IOException e) {
            // Cleanup on failure
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException ex) {
                log.warn("Failed to cleanup temp file after error: {}", tempFile, ex);
            }
            throw e;
        }
    }
    
    /**
     * Creates a FileRefValue by serializing an object to JSON.
     * 
     * @param obj object to serialize
     * @param targetDir directory to store temp files
     * @param prefix filename prefix
     * @return new FileRefValue
     * @throws IOException if serialization or writing fails
     */
    public static FileRefValue fromObject(Object obj, Path targetDir, String prefix) throws IOException {
        Files.createDirectories(targetDir);
        Path tempFile = Files.createTempFile(targetDir, prefix, ".json");
        
        try (OutputStream out = Files.newOutputStream(tempFile)) {
            ObjectConversion.getObjectMapper().writeValue(out, obj);
            return new FileRefValue(tempFile, "application/json");
        } catch (IOException e) {
            // Cleanup on failure
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException ex) {
                log.warn("Failed to cleanup temp file after error: {}", tempFile, ex);
            }
            throw e;
        }
    }
    
    /**
     * Creates a FileRefValue by writing bytes to a temp file.
     * 
     * @param data bytes to write
     * @param mediaType content type
     * @param targetDir directory to store temp files
     * @param prefix filename prefix
     * @return new FileRefValue
     * @throws IOException if writing fails
     */
    public static FileRefValue fromBytes(byte[] data, String mediaType, 
                                        Path targetDir, String prefix) throws IOException {
        Files.createDirectories(targetDir);
        String suffix = mediaType != null ? guessSuffix(mediaType) : ".dat";
        Path tempFile = Files.createTempFile(targetDir, prefix, suffix);
        
        try {
            Files.write(tempFile, data);
            return new FileRefValue(tempFile, mediaType);
        } catch (IOException e) {
            // Cleanup on failure
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException ex) {
                log.warn("Failed to cleanup temp file after error: {}", tempFile, ex);
            }
            throw e;
        }
    }
    
    @Override
    public RefValueType getType() {
        return RefValueType.FILE;
    }
    
    @Override
    public long getSize() {
        return size;
    }
    
    @Override
    public Optional<String> getMediaType() {
        return Optional.ofNullable(mediaType);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public <T> T read(Class<T> targetType) throws IOException {
        if (released.get()) {
            throw new IOException("File has been released: " + filePath);
        }
        
        // Handle text/plain as String (UTF-8)
        if (mediaType != null && mediaType.startsWith("text/plain")) {
            byte[] bytes = Files.readAllBytes(filePath);
            String text = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
            if (targetType == Object.class || targetType == String.class) {
                return (T) text;
            }
            // If requesting a different type, try to convert
            if (targetType == byte[].class) {
                return (T) bytes;
            }
        }
        
        // Handle JSON or other types with Jackson
        try (InputStream in = Files.newInputStream(filePath)) {
            return ObjectConversion.getObjectMapper().readValue(in, targetType);
        }
    }
    
    @Override
    public <R> R read(ReadFunction<R> reader) throws IOException {
        if (released.get()) {
            throw new IOException("File has been released: " + filePath);
        }
        return reader.apply(new FileRefValueAccess());
    }
    
    @Override
    public InputStream openStream() throws IOException {
        if (released.get()) {
            throw new IOException("File has been released: " + filePath);
        }
        return Files.newInputStream(filePath);
    }
    
    @Override
    public RefValueDescriptor toDescriptor() {
        return RefValueDescriptor.builder()
                .type(RefValueType.FILE)
                .locator(filePath.toString())
                .mediaType(mediaType)
                .size(size)
                .build();
    }
    
    @Override
    public void onRelease() {
        if (released.compareAndSet(false, true)) {
            try {
                if (Files.deleteIfExists(filePath)) {
                    log.debug("Deleted temp file: {} (size: {} bytes)", filePath.getFileName(), size);
                } else {
                    log.warn("Temp file already deleted: {}", filePath);
                }
            } catch (IOException e) {
                log.error("Failed to delete temp file: {}", filePath, e);
            }
        }
    }

    /**
     * Checks if this RefValue has been released.
     * 
     * @return true if released (file deleted)
     */
    public boolean isReleased() {
        return released.get();
    }
    
    /**
     * Guesses file suffix from media type.
     */
    private static String guessSuffix(String mediaType) {
        if (mediaType == null) return ".dat";
        if (mediaType.contains("json")) return ".json";
        if (mediaType.contains("xml")) return ".xml";
        if (mediaType.contains("text")) return ".txt";
        if (mediaType.contains("html")) return ".html";
        if (mediaType.contains("pdf")) return ".pdf";
        if (mediaType.contains("jpeg") || mediaType.contains("jpg")) return ".jpg";
        if (mediaType.contains("png")) return ".png";
        if (mediaType.contains("gif")) return ".gif";
        if (mediaType.contains("mp4")) return ".mp4";
        if (mediaType.contains("zip")) return ".zip";
        return ".dat";
    }
    
    /**
     * Access helper for file-backed values.
     */
    private class FileRefValueAccess implements RefValueAccess {
        
        @Override
        public <T> T asObject(Class<T> targetType) throws IOException {
            return read(targetType);
        }
        
        @Override
        public JsonNode asJsonTree() throws IOException {
            if (released.get()) {
                throw new IOException("File has been released: " + filePath);
            }
            try (InputStream in = Files.newInputStream(filePath)) {
                return ObjectConversion.getObjectMapper().readTree(in);
            }
        }
        
        @Override
        public JsonNode jsonAt(String pointer) throws IOException {
            JsonNode tree = asJsonTree();
            return tree.at(pointer);
        }
        
        @Override
        public InputStream streamBytes() throws IOException {
            return openStream();
        }
        
        @Override
        public long getSize() {
            return size;
        }
    }
}
