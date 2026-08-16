package com.joe.taskira.attachments.adapter;

import com.joe.taskira.attachments.port.DocumentStorage;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.regex.Pattern;

@Component
public class LocalFileSystemStorage implements DocumentStorage {

    private static final Pattern STORAGE_KEY_PATTERN =
            Pattern.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    private final Path baseDirectory;

    public LocalFileSystemStorage(@Value("${app.attachments.storage-path}") String storagePath) {
        this.baseDirectory = Path.of(storagePath).toAbsolutePath().normalize();
    }

    @PostConstruct
    void ensureBaseDirectoryExists() throws IOException {
        Files.createDirectories(baseDirectory);
    }

    @Override
    public String store(InputStream content) throws IOException {
        String storageKey = UUID.randomUUID().toString();
        Files.copy(content, resolve(storageKey), StandardCopyOption.REPLACE_EXISTING);
        return storageKey;
    }

    @Override
    public InputStream retrieve(String storageKey) throws IOException {
        return Files.newInputStream(resolve(storageKey));
    }

    @Override
    public void delete(String storageKey) throws IOException {
        Files.deleteIfExists(resolve(storageKey));
    }

    // Defense in depth: even though every caller today only ever passes back a key this
    // same class generated, a stored value ending up in a filesystem path must never be
    // trusted blindly - validate the shape first, then confirm the resolved path is
    // still inside baseDirectory before touching the filesystem.
    private Path resolve(String storageKey) {
        if (!STORAGE_KEY_PATTERN.matcher(storageKey).matches()) {
            throw new IllegalArgumentException("Invalid storage key");
        }
        Path resolved = baseDirectory.resolve(storageKey).normalize();
        if (!resolved.startsWith(baseDirectory)) {
            throw new IllegalArgumentException("Storage key resolves outside the storage directory");
        }
        return resolved;
    }
}
