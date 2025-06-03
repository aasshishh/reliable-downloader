package com.accurx.reliabledownloader.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class Md5Tests {

    @TempDir // JUnit 5 annotation for creating a temporary directory
    Path tempDir;

    @Test
    @DisplayName("should calculate correct MD5 for an empty file")
    void contentMd5_emptyFile_returnsCorrectHash() throws IOException {
        Path emptyFilePath = tempDir.resolve("empty.txt");
        Files.createFile(emptyFilePath);

        // MD5 hash for an empty string (Base64 encoded)
        // You can verify this using online MD5 calculators or common utilities
        String expectedMd5 = "1B2M2Y8AsgTpgAmY7PhCfg==";
        String actualMd5 = Md5.contentMd5(emptyFilePath.toFile());

        assertEquals(expectedMd5, actualMd5);
    }

    @Test
    @DisplayName("should calculate correct MD5 for a file with content")
    void contentMd5_fileWithContent_returnsCorrectHash() throws IOException {
        Path filePath = tempDir.resolve("test.txt");
        String content = "Hello World!";
        Files.writeString(filePath, content);

        // The correct Base64-encoded MD5 for "Hello World!"
        // When calculated with Guava's implementation
        String expectedMd5 = "7Qdih1MuhjZehB6Sv8UNjA==";

        String actualMd5 = Md5.contentMd5(filePath.toFile());

        assertEquals(expectedMd5, actualMd5);
    }

    @Test
    @DisplayName("should throw IOException if file does not exist")
    void contentMd5_nonExistentFile_throwsIOException() {
        File nonExistentFile = tempDir.resolve("nonexistent.txt").toFile();

        assertThrows(IOException.class, () -> Md5.contentMd5(nonExistentFile));
    }

    @Test
    @DisplayName("should handle large files efficiently (conceptual test)")
    void contentMd5_largeFile_performanceTest() throws IOException {
        Path largeFilePath = tempDir.resolve("large.bin");

        // Create a 1MB file with a specific pattern to ensure consistent MD5
        byte[] pattern = "Test pattern for MD5 calculation".getBytes();
        byte[] largeContent = new byte[1024 * 1024]; // 1 MB
        for (int i = 0; i < largeContent.length; i++) {
            largeContent[i] = pattern[i % pattern.length];
        }
        Files.write(largeFilePath, largeContent);

        String actualMd5 = Md5.contentMd5(largeFilePath.toFile());

        // Assert that an MD5 was generated and it's not empty, indicating successful processing
        assertNotNull(actualMd5);
        assertTrue(actualMd5.length() > 0, "MD5 string should not be empty");
    }

}