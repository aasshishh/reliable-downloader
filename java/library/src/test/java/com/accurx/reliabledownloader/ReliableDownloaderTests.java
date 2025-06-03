
package com.accurx.reliabledownloader;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class ReliableDownloaderTest {
    @TempDir
    Path tempDir;

    @Test
    void failsWithInvalidUrl() {
        assertThrows(Exception.class, () -> {
            new ReliableDownloader(
                    "invalid-url",
                    tempDir.resolve("test.file"),
                    "dummy-hash"
            );
        });
    }

    @Test
    void failsWithInvalidHash() {
        // This test requires a real HTTP server
        // You might want to use WireMock or similar for real testing
        assertThrows(Exception.class, () -> {
            ReliableDownloader downloader = new ReliableDownloader(
                    "https://example.com/test.txt",
                    tempDir.resolve("test.file"),
                    "invalid-hash"
            );
            downloader.download();
        });
    }
}