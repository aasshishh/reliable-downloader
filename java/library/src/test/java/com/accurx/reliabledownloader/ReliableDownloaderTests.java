package com.accurx.reliabledownloader;

import com.accurx.reliabledownloader.core.DownloaderConfig;
import com.accurx.reliabledownloader.impl.ReliableDownloader;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ReliableDownloaderTests {
    @TempDir
    Path tempDir;

    @Test
    void failsWithInvalidUrl() {
        // Create a downloader with default config
        ReliableDownloader downloader = new ReliableDownloader(DownloaderConfig.getDefault());

        // Test with invalid URI
        assertThrows(Exception.class, () -> {
            URI invalidUri = URI.create("invalid-url");
            try (FileOutputStream outputStream = new FileOutputStream(tempDir.resolve("test.file").toFile())) {
                downloader.downloadFile(invalidUri, outputStream);
            }
        });
    }

    @Test
    @DisplayName("should throw IOException when URL returns connection error")
    void failsWithNonExistentUrl() {
        // Create a downloader with default config but with hash verification enabled
        // Set maxRetries to 0 for this test to ensure the failure occurs quickly
        DownloaderConfig config = DownloaderConfig.builder()
                .verifyHash(true)
                .maxRetries(0)
                .build();
        ReliableDownloader downloader = new ReliableDownloader(config);

        // Test with a URL that's guaranteed to cause a connection failure (non-routable local address)
        URI nonExistentUri = URI.create("http://localhost:54321/non-existent-file.txt");

        IOException thrown = assertThrows(IOException.class, () -> {
            try (FileOutputStream outputStream = new FileOutputStream(tempDir.resolve("test.file").toFile())) {
                downloader.downloadFile(nonExistentUri, outputStream);
            }
        });

        // The exception should now contain a more specific message after retries (even if 0 retries)
        // because initializeDownload attempts to connect and fails, then wraps the original exception.
        // The message will be "Failed to initialize download after X retries."
        // We also check for the original connection messages in case the retry logic is bypassed
        // or a different phase throws the exception.
        assertTrue(thrown.getMessage().contains("Failed to initialize download after 0 retries.") || // New specific message
                thrown.getMessage().contains("Could not determine file size") || // From initializeDownload if content-length is -1
                thrown.getMessage().contains("Connection refused") ||
                thrown.getMessage().contains("Failed to connect") ||
                thrown.getMessage().contains("connect failed"));
    }
}

