package com.joe.taskira.attachments.port;

import java.io.IOException;
import java.io.InputStream;

/**
 * Port (ADR-0009/ADR-0021): LocalFileSystemStorage is the only adapter today. MinIO or
 * an object store would implement this same interface without changing any caller.
 */
public interface DocumentStorage {

    /**
     * Persists the stream's content under a new, server-generated key and returns it.
     * Callers never choose the key - eliminates path traversal by construction, not by
     * validating a client-supplied name.
     */
    String store(InputStream content) throws IOException;

    InputStream retrieve(String storageKey) throws IOException;

    void delete(String storageKey) throws IOException;
}
