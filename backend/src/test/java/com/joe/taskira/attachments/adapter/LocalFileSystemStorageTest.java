package com.joe.taskira.attachments.adapter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalFileSystemStorageTest {

    @TempDir
    private Path tempDir;

    private LocalFileSystemStorage storage;

    @BeforeEach
    void setUp() throws Exception {
        storage = new LocalFileSystemStorage(tempDir.toString());
        storage.ensureBaseDirectoryExists();
    }

    @Test
    void storeThenRetrieveRoundTripsTheExactContent() throws Exception {
        byte[] content = "real file content for a real round trip".getBytes(StandardCharsets.UTF_8);

        String storageKey = storage.store(new ByteArrayInputStream(content));

        try (InputStream retrieved = storage.retrieve(storageKey)) {
            assertThat(retrieved.readAllBytes()).isEqualTo(content);
        }
    }

    @Test
    void eachStoreCallGetsItsOwnServerGeneratedKeyNeverDerivedFromCallerInput() throws Exception {
        String key1 = storage.store(new ByteArrayInputStream("a".getBytes(StandardCharsets.UTF_8)));
        String key2 = storage.store(new ByteArrayInputStream("b".getBytes(StandardCharsets.UTF_8)));

        assertThat(key1).isNotEqualTo(key2);
        assertThat(key1).matches("[0-9a-fA-F-]{36}");
    }

    @Test
    void deleteRemovesTheStoredFile() throws Exception {
        String storageKey = storage.store(new ByteArrayInputStream("to be deleted".getBytes(StandardCharsets.UTF_8)));
        assertThat(Files.list(tempDir)).hasSize(1);

        storage.delete(storageKey);

        assertThat(Files.list(tempDir)).isEmpty();
    }

    @Test
    void retrieveRejectsAPathTraversalAttemptRatherThanEscapingTheStorageDirectory() {
        assertThatThrownBy(() -> storage.retrieve("../../../../etc/passwd"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void retrieveRejectsAKeyThatIsNotShapedLikeAGeneratedUuid() {
        assertThatThrownBy(() -> storage.retrieve("not-a-uuid-at-all"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> storage.retrieve("..%2F..%2Fetc%2Fpasswd"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deleteOfANonExistentKeyIsANoOpRatherThanAnError() throws Exception {
        String neverStoredButValidShapeKey = java.util.UUID.randomUUID().toString();
        storage.delete(neverStoredButValidShapeKey);
        // No exception - deleting something already gone is not itself an error.
    }
}
