package com.accurx.reliabledownloader.core;

import com.accurx.reliabledownloader.util.DownloadProgressObserver;
import com.accurx.reliabledownloader.util.Md5;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileDownloadCommandTests {

    @Mock
    private FileDownloader mockFileDownloader;
    @Mock
    private DownloadProgressObserver mockObserver; // Mock for observer
    @TempDir
    Path tempDir;

    private FileDownloadSettings downloadSettings;
    private Path destinationFilePath; // The final destination path
    private FileDownloadCommand command;

    @BeforeEach
    void setUp() throws IOException {
        destinationFilePath = tempDir.resolve("downloaded_file.txt");

        // Ensure parent directory exists for the final destination path
        Files.createDirectories(destinationFilePath.getParent());

        downloadSettings = new FileDownloadSettings(
                URI.create("http://example.com/testfile.txt"),
                destinationFilePath,
                true
        );
        command = new FileDownloadCommand(mockFileDownloader, downloadSettings);
    }

    @Test
    @DisplayName("should call downloadFile on FileDownloader and verify MD5 if present and matches")
    void run_successfulDownloadWithMatchingMd5_verifiesIntegrity() throws Exception {
        String expectedMd5 = "someBase64Md5";
        byte[] testContent = "test content".getBytes();
        Path expectedTempFilePath = Path.of(destinationFilePath.toString() + ".tmp");

        when(mockFileDownloader.downloadFile(any(URI.class), any(OutputStream.class), anyLong()))
                .thenAnswer(invocation -> {
                    OutputStream os = invocation.getArgument(1);
                    os.write(testContent);
                    os.flush(); // Ensure content is written
                    return Optional.of(expectedMd5);
                });

        try (MockedStatic<Md5> mockedMd5 = mockStatic(Md5.class)) {
            // Mock Md5.contentMd5 to return the expected MD5 when called with the temp file
            mockedMd5.when(() -> Md5.contentMd5(eq(expectedTempFilePath.toFile()))).thenReturn(expectedMd5);

            command.run();

            // Verify that downloadFile was called with the correct arguments
            verify(mockFileDownloader)
                    .downloadFile(eq(downloadSettings.sourceUrl()), any(OutputStream.class), eq(0L));
            // Verify that the final file exists and is correctly named
            assertTrue(Files.exists(destinationFilePath));
            assertFalse(Files.exists(expectedTempFilePath)); // Temp file should be deleted
            assertEquals(testContent.length, Files.size(destinationFilePath)); // Verify content size
        }
    }

    @Test
    @DisplayName("should call downloadFile on FileDownloader and log warning if MD5 not present")
    void run_successfulDownloadWithoutMd5_logsWarning() throws Exception {
        byte[] testContent = "test content without md5".getBytes();
        Path expectedTempFilePath = Path.of(destinationFilePath.toString() + ".tmp");

        when(mockFileDownloader.downloadFile(any(URI.class), any(OutputStream.class), anyLong())) // Added anyLong()
                .thenAnswer(invocation -> {
                    OutputStream os = invocation.getArgument(1);
                    os.write(testContent);
                    os.flush();
                    return Optional.empty(); // No MD5 returned
                });

        command.run();

        verify(mockFileDownloader).downloadFile(eq(downloadSettings.sourceUrl()), any(OutputStream.class), eq(0L));
        assertTrue(Files.exists(destinationFilePath));
        assertFalse(Files.exists(expectedTempFilePath));
        assertEquals(testContent.length, Files.size(destinationFilePath));
    }


    @Test
    @DisplayName("should call downloadFile on FileDownloader and log warning if MD5 present but mismatch")
    void run_successfulDownloadWithMismatchedMd5_throwsIOExceptionAndCleansUp() throws Exception {
        String expectedMd5 = "correctBase64Md5";
        String mismatchedMd5 = "wrongBase64Md5";
        byte[] testContent = "test content for mismatch".getBytes();
        Path expectedTempFilePath = Path.of(destinationFilePath.toString() + ".tmp");

        when(mockFileDownloader.downloadFile(any(URI.class), any(OutputStream.class), anyLong()))
                .thenAnswer(invocation -> {
                    OutputStream os = invocation.getArgument(1);
                    os.write(testContent);
                    os.flush();
                    return Optional.of(expectedMd5); // Downloader returns what it believes is correct
                });

        try (MockedStatic<Md5> mockedMd5 = mockStatic(Md5.class)) {
            // Mock Md5.contentMd5 to return a Mismatched MD5
            mockedMd5.when(() -> Md5.contentMd5(eq(expectedTempFilePath.toFile()))).thenReturn(mismatchedMd5);

            IOException thrown = assertThrows(IOException.class, () -> command.run());

            assertTrue(thrown.getMessage().contains("MD5 integrity check failed."));
            verify(mockFileDownloader).downloadFile(eq(downloadSettings.sourceUrl()), any(OutputStream.class), eq(0L));
            assertFalse(Files.exists(destinationFilePath)); // Final file should not exist
            assertFalse(Files.exists(expectedTempFilePath)); // Temp file should be deleted
        }
    }

    @Test
    @DisplayName("should rethrow exception from FileDownloader and clean up temporary file")
    void run_fileDownloaderThrowsException_rethrowsAndCleansUpTempFile() throws Exception {
        Path expectedTempFilePath = Path.of(destinationFilePath.toString() + ".tmp");

        // Create a dummy temp file to ensure cleanup logic is tested
        Files.createFile(expectedTempFilePath);
        assertTrue(Files.exists(expectedTempFilePath));

        // Configure mockFileDownloader to throw an exception
        when(mockFileDownloader.downloadFile(any(URI.class), any(OutputStream.class), anyLong()))
                .thenThrow(new IOException("Simulated download error"));

        IOException thrown = assertThrows(IOException.class, () -> command.run());

        assertEquals("Simulated download error", thrown.getMessage());
        verify(mockFileDownloader).downloadFile(eq(downloadSettings.sourceUrl()), any(OutputStream.class), eq(0L));
        assertFalse(Files.exists(expectedTempFilePath)); // Temp file should be cleaned up
        assertFalse(Files.exists(destinationFilePath)); // Final file should not exist
    }

    @Test
    @DisplayName("should resume download if temporary file exists")
    void run_resumesDownloadIfTempFileExists() throws Exception {
        long existingFileSize = 100L;
        byte[] additionalContent = "additional content".getBytes();
        String expectedMd5 = "resumeMd5";
        Path tempFile = Path.of(destinationFilePath.toString() + ".tmp");

        // Create a mock temporary file with some content
        Files.createFile(tempFile);
        Files.write(tempFile, new byte[(int) existingFileSize]);
        assertEquals(existingFileSize, Files.size(tempFile));


        when(mockFileDownloader.downloadFile(any(URI.class), any(OutputStream.class), eq(existingFileSize)))
                .thenAnswer(invocation -> {
                    OutputStream os = invocation.getArgument(1);
                    os.write(additionalContent);
                    os.flush();
                    return Optional.of(expectedMd5);
                });

        try (MockedStatic<Md5> mockedMd5 = mockStatic(Md5.class)) {
            mockedMd5.when(() -> Md5.contentMd5(eq(tempFile.toFile()))).thenReturn(expectedMd5);

            command.run();

            verify(mockFileDownloader)
                    .downloadFile(eq(downloadSettings.sourceUrl()), any(OutputStream.class), eq(existingFileSize));
            assertTrue(Files.exists(destinationFilePath));
            assertFalse(Files.exists(tempFile));
            assertEquals(existingFileSize + additionalContent.length, Files.size(destinationFilePath));
        }
    }

    @Test
    @DisplayName("should handle IOException during temp file cleanup gracefully")
    void run_tempFileCleanupFails_logsErrorAndContinues() throws Exception {
        String expectedMd5 = "cleanupFailMd5";
        byte[] testContent = "cleanup fail content".getBytes();
        Path expectedTempFilePath = Path.of(destinationFilePath.toString() + ".tmp");

        when(mockFileDownloader.downloadFile(any(URI.class), any(OutputStream.class), anyLong()))
                .thenThrow(new IOException("Simulated download error for cleanup test"));

        try (MockedStatic<Files> mockedFiles = mockStatic(Files.class, CALLS_REAL_METHODS)) {
            // Ensure temp file exists for the delete attempt
            Files.createFile(expectedTempFilePath);
            assertTrue(Files.exists(expectedTempFilePath));

            mockedFiles.when(() -> Files.deleteIfExists(eq(expectedTempFilePath)))
                    .thenThrow(new IOException("Simulated cleanup error"));

            IOException thrown = assertThrows(IOException.class, () -> command.run());

            assertEquals("Simulated download error for cleanup test", thrown.getMessage());
            verify(mockFileDownloader)
                    .downloadFile(eq(downloadSettings.sourceUrl()), any(OutputStream.class), eq(0L));
            assertTrue(Files.exists(expectedTempFilePath));
            assertFalse(Files.exists(destinationFilePath)); // Final destination should not exist
        }
    }
}
