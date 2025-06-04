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
                destinationFilePath, // Use the actual destination path for settings
                "ReliableDownloader"
        );
        command = new FileDownloadCommand(mockFileDownloader, downloadSettings);
    }

    @Test
    @DisplayName("should call downloadFile on FileDownloader and verify MD5 if present and matches")
    void run_successfulDownloadWithMatchingMd5_verifiesIntegrity() throws Exception {
        String expectedMd5 = "someBase64Md5";
        byte[] testContent = "test content".getBytes();
        Path expectedTempFilePath = Path.of(destinationFilePath.toString() + ".tmp"); // The temp path FileDownloadCommand will use

        when(mockFileDownloader.downloadFile(any(URI.class), any(OutputStream.class)))
                .thenAnswer(invocation -> {
                    // Get the OutputStream that FileDownloadCommand provides (which will be for the .tmp file)
                    OutputStream os = invocation.getArgument(1);
                    os.write(testContent);
                    os.flush(); // Ensure content is written
                    return Optional.of(expectedMd5);
                });

        try (MockedStatic<Md5> mockedMd5 = mockStatic(Md5.class)) {
            // Mock Md5.contentMd5 to return the expected MD5 when called with the temp file
            mockedMd5.when(() -> Md5.contentMd5(eq(expectedTempFilePath.toFile()))).thenReturn(expectedMd5);

            command.run();

            verify(mockFileDownloader).downloadFile(any(URI.class), any(OutputStream.class));
            // Verify Md5.contentMd5 was called with the correct temporary file path
            mockedMd5.verify(() -> Md5.contentMd5(eq(expectedTempFilePath.toFile())));
            verifyNoMoreInteractions(mockFileDownloader);

            // Assert that the final destination file exists and contains the content
            // This is crucial to verify the atomic move
            assertEquals(new String(testContent), Files.readString(destinationFilePath));
            // Ensure the temporary file is gone
            assert(!Files.exists(expectedTempFilePath));
        }
    }

    @Test
    @DisplayName("should call downloadFile on FileDownloader and log warning if MD5 not present")
    void run_successfulDownloadNoMd5_logsWarning() throws Exception {
        byte[] testContent = "some content for no MD5".getBytes();
        Path expectedTempFilePath = Path.of(destinationFilePath.toString() + ".tmp"); // The temp path FileDownloadCommand will use

        when(mockFileDownloader.downloadFile(any(URI.class), any(OutputStream.class)))
                .thenAnswer(invocation -> {
                    OutputStream os = invocation.getArgument(1);
                    os.write(testContent);
                    os.flush();
                    return Optional.empty(); // Simulate no MD5 returned
                });

        try (MockedStatic<Md5> mockedMd5 = mockStatic(Md5.class)) {
            // Md5.contentMd5 should NOT be called if contentMd5Opt is empty.
            mockedMd5.verify(() -> Md5.contentMd5(any(File.class)), never()); // Verify it's never called

            command.run();

            verify(mockFileDownloader).downloadFile(any(URI.class), any(OutputStream.class));
            verifyNoMoreInteractions(mockFileDownloader);

            // Assert that the final destination file exists and contains the content
            assertEquals(new String(testContent), Files.readString(destinationFilePath));
            // Ensure the temporary file is gone
            assert(!Files.exists(expectedTempFilePath));
        }
    }

    @Test
    @DisplayName("should call downloadFile on FileDownloader and log warning if MD5 present but mismatch")
    void run_successfulDownloadWithMismatchingMd5_logsError() throws Exception {
        String expectedMd5FromDownloader = "correctMd5FromDownloader";
        String computedMd5FromFile = "incorrectMd5FromFile";
        byte[] testContent = "content for mismatch".getBytes();
        Path expectedTempFilePath = Path.of(destinationFilePath.toString() + ".tmp"); // The temp path FileDownloadCommand will use

        when(mockFileDownloader.downloadFile(any(URI.class), any(OutputStream.class)))
                .thenAnswer(invocation -> {
                    OutputStream os = invocation.getArgument(1);
                    os.write(testContent);
                    os.flush();
                    return Optional.of(expectedMd5FromDownloader);
                });

        try (MockedStatic<Md5> mockedMd5 = mockStatic(Md5.class)) {
            // Mock Md5.contentMd5 to return a *mismatching* MD5 when called with the temp file
            mockedMd5.when(() -> Md5.contentMd5(eq(expectedTempFilePath.toFile()))).thenReturn(computedMd5FromFile);

            // Expect an IOException due to MD5 mismatch
            IOException thrown = assertThrows(IOException.class, () -> command.run());
            assertEquals("MD5 integrity check failed.", thrown.getMessage());

            verify(mockFileDownloader).downloadFile(any(URI.class), any(OutputStream.class));
            // Verify Md5.contentMd5 was called with the correct temporary file path
            mockedMd5.verify(() -> Md5.contentMd5(eq(expectedTempFilePath.toFile())));
            verifyNoMoreInteractions(mockFileDownloader);

            // Assert that neither the final destination nor the temporary file exist
            assert(!Files.exists(destinationFilePath));
            assert(!Files.exists(expectedTempFilePath));
        }
    }

    @Test
    @DisplayName("should rethrow exception from FileDownloader and clean up temporary file")
    void run_downloaderThrowsException_rethrowsAndCleansUpTempFile() throws Exception {
        IOException thrownByDownloader = new IOException("Simulated download error");
        Path expectedTempFilePath = Path.of(destinationFilePath.toString() + ".tmp"); // The temp path FileDownloadCommand will use

        // Simulate creation of a temporary file before the download fails
        Files.createFile(expectedTempFilePath); // Pre-create the temp file for cleanup test

        when(mockFileDownloader.downloadFile(any(URI.class), any(OutputStream.class)))
                .thenThrow(thrownByDownloader);

        IOException thrown = assertThrows(IOException.class, () -> command.run());
        assertEquals(thrownByDownloader.getMessage(), thrown.getMessage());

        try (MockedStatic<Md5> mockedMd5 = mockStatic(Md5.class)) {
            mockedMd5.verify(() -> Md5.contentMd5(any(File.class)), never());
        }

        verify(mockFileDownloader).downloadFile(any(URI.class), any(OutputStream.class));
        verifyNoMoreInteractions(mockFileDownloader);

        // Assert that the temporary file is deleted after the exception
        assert(!Files.exists(expectedTempFilePath));
        // Assert that the final destination file does not exist
        assert(!Files.exists(destinationFilePath));
    }
}
