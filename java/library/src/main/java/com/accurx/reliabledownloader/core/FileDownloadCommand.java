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

    public void run() throws Exception {
        Path destinationFilePath = downloadSettings.destinationFilePath();
        Path tempFilePath = Path.of(destinationFilePath.toString() + ".tmp");

        LOGGER.info(
                "Starting download from {} to {}...",
                downloadSettings.sourceUrl(),
                destinationFilePath.toAbsolutePath()
        );

        Optional<String> contentMd5Opt;

        try {
            // Ensure the parent directory exists for the temporary file
            Files.createDirectories(tempFilePath.getParent());

            try (OutputStream outputStream =
                         Files.newOutputStream(
                                 tempFilePath,
                                 StandardOpenOption.TRUNCATE_EXISTING,
                                 StandardOpenOption.CREATE)) {
                contentMd5Opt = fileDownloader.downloadFile(downloadSettings.sourceUrl(), outputStream);
            }

            // Download successful, now verify MD5 if present
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

        } catch (Exception e) {
            LOGGER.error("Download failed or interrupted. Attempting to clean up temporary file {}: {}",
                    tempFilePath.getFileName(), e.getMessage(), e); // Added 'e' to log the stack trace
            try {
                Files.deleteIfExists(tempFilePath); // Ensure temporary file is deleted on any exception
            } catch (IOException cleanupException) {
                LOGGER.error("Failed to delete temporary file {}: {}",
                        tempFilePath.getFileName(), cleanupException.getMessage(), cleanupException); // Added 'cleanupException' for stack trace
            }
            throw e; // Re-throw the original exception
        }
    }
}
