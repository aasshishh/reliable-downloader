package com.accurx.reliabledownloader.core;

import com.accurx.reliabledownloader.util.Md5;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;

public class FileDownloadCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileDownloadCommand.class);

    private final FileDownloader fileDownloader;
    private final FileDownloadSettings downloadSettings;

    public FileDownloadCommand(FileDownloader fileDownloader, FileDownloadSettings downloadSettings) {
        this.fileDownloader = fileDownloader;
        this.downloadSettings = downloadSettings;
    }

    /**
     * Executes the file download command, handling resume and retries.
     * @throws Exception if the download fails after all retries.
     */
    public void run() throws Exception {
        Path tempFilePath = Path.of(downloadSettings.destinationFilePath().toString() + ".tmp");
        int maxRetries = 1; // Only one retry for RangeNotSupportedException to start from scratch
        int currentAttempt = 0; // Renamed from currentRetry for clarity in this context

        LOGGER.info(
                "Starting download from {} to {}...",
                downloadSettings.sourceUrl(),
                downloadSettings.destinationFilePath().toAbsolutePath()
        );

        while (currentAttempt <= maxRetries) {
            try {
                performDownloadAttempt(tempFilePath);
                return;
            } catch (RangeNotSupportedException e) {
                LOGGER.warn("Caught RangeNotSupportedException");
                if (currentAttempt < maxRetries) {
                    LOGGER.warn("Attempted to resume download but server does not support range requests. " +
                                    "Cleaning up temporary file and retrying from scratch (attempt {}/{})",
                            currentAttempt + 1, maxRetries);
                    Files.deleteIfExists(tempFilePath); // Clean up the incomplete file
                    currentAttempt++;
                } else {
                    LOGGER.error("Download failed after retrying. Server does not support range requests: {}", e.getMessage(), e);
                    throw e;
                }
            } catch (Exception e) { // Catch any other download-related exceptions
                LOGGER.error("Download failed or interrupted. Attempting to clean up temporary file {}: {}",
                        tempFilePath.getFileName(), e.getMessage(), e);
                try {
                    Files.deleteIfExists(tempFilePath);
                } catch (IOException cleanupException) {
                    LOGGER.error("Failed to delete temporary file {}: {}",
                            tempFilePath.getFileName(), cleanupException.getMessage(), cleanupException);
                }
                throw e;
            }
        }
    }

    /**
     * Performs a single attempt of the file download, including checking for existing temp files,
     * determining start offset, executing the download, and performing MD5 verification.
     *
     * @param tempFilePath The path to the temporary download file.
     * @throws Exception if the download attempt fails.
     */
    private void performDownloadAttempt(Path tempFilePath) throws Exception {
        long startOffset = 0;
        Path destinationFilePath = downloadSettings.destinationFilePath();

        if (Files.exists(tempFilePath)) {
            startOffset = Files.size(tempFilePath);
            LOGGER.info("Resuming download from offset: {} bytes for file: {}", startOffset, tempFilePath.getFileName());
        } else {
            LOGGER.info("No temporary file found, starting new download.");
        }

        // Ensure the parent directory exists for the temporary file
        Files.createDirectories(tempFilePath.getParent());

        // Open the output stream in append mode if resuming, otherwise truncate/create
        StandardOpenOption openOption = StandardOpenOption.CREATE;
        if (startOffset > 0) {
            openOption = StandardOpenOption.APPEND;
        }

        Optional<String> contentMd5Opt;
        try (OutputStream outputStream = Files.newOutputStream(tempFilePath, openOption)) {
            // Pass the determined startOffset to the downloader
            contentMd5Opt = fileDownloader.downloadFile(downloadSettings.sourceUrl(), outputStream, startOffset);
        }

        // MD5 Verification (Moved into this method for encapsulation)
        boolean md5Verified = false;
        if (contentMd5Opt.isPresent()) {
            String expectedMd5 = contentMd5Opt.get();
            String computedMd5 = Md5.contentMd5(tempFilePath.toFile()); // Compute MD5 of the temp file
            if (expectedMd5.equals(computedMd5)) {
                LOGGER.info("MD5 hash is present, download integrity verified.");
                md5Verified = true;
            } else {
                LOGGER.error("MD5 verification failed! Expected MD5={}, found MD5={} for temporary file {}. " +
                                "Deleting incomplete download.",
                        expectedMd5, computedMd5, tempFilePath.getFileName());
                Files.deleteIfExists(tempFilePath); // Delete the temporary file if MD5 fails
                throw new IOException("MD5 integrity check failed.");
            }
        } else {
            LOGGER.warn("MD5 hash is not present, download integrity was not verified.");
            md5Verified = true; // Consider it verified if no MD5 provided
        }

        // If MD5 is verified (or not applicable), rename the temporary file to the final destination
        if (md5Verified) {
            LOGGER.info("Download complete. Attempting to move temporary file {} to final destination {}.",
                    tempFilePath.getFileName(), destinationFilePath.getFileName());
            try {
                Files.move(
                        tempFilePath,
                        destinationFilePath,
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                        java.nio.file.StandardCopyOption.ATOMIC_MOVE);
                LOGGER.info("File moved successfully. Download Complete!");
            } catch (IOException moveException) {
                LOGGER.error("Failed to move temporary file {} to final destination {}: {}",
                        tempFilePath.getFileName(), destinationFilePath.getFileName(), moveException.getMessage(), moveException);
                throw new IOException("Failed to finalize download: Could not move temporary file to destination.", moveException);
            }
        } else {
            LOGGER.error("Download did not complete successfully. Temporary file {} might remain or was deleted.",
                    tempFilePath.getFileName());
        }
    }
}
