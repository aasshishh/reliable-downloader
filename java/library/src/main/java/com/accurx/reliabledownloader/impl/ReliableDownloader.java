
package com.accurx.reliabledownloader.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class ReliableDownloader {
    // Configuration
    private final int CHUNK_SIZE = 1024 * 1024; // 1MB chunks
    private final int MAX_RETRIES = 3;
    private final int RETRY_DELAY_MS = 5000; // 5 seconds

    private final String downloadUrl;
    private final Path destinationPath;
    private final String expectedHash;
    private final long totalSize;

    // State tracking
    private long downloadedBytes = 0;
    private boolean supportsRangeRequests = false;

    public ReliableDownloader(String downloadUrl, Path destinationPath, String expectedHash) throws IOException {
        this.downloadUrl = downloadUrl;
        this.destinationPath = destinationPath;
        this.expectedHash = expectedHash;
        this.totalSize = determineFileSize();
    }

    private long determineFileSize() throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(downloadUrl).openConnection();
        conn.setRequestMethod("HEAD");
        conn.connect();

        // Check if server supports range requests
        String acceptRanges = conn.getHeaderField("Accept-Ranges");
        this.supportsRangeRequests = acceptRanges != null && acceptRanges.equals("bytes");

        long size = conn.getContentLengthLong();
        conn.disconnect();

        if (size == -1) {
            throw new IOException("Could not determine file size");
        }
        return size;
    }

    private long getResumePosition() {
        if (Files.exists(destinationPath)) {
            try {
                return Files.size(destinationPath);
            } catch (IOException e) {
                return 0;
            }
        }
        return 0;
    }

    public void download() throws IOException {
        downloadedBytes = getResumePosition();

        while (downloadedBytes < totalSize) {
            try {
                downloadChunk();
            } catch (IOException e) {
                handleDownloadError(e);
            }
        }

        verifyDownload();
    }

    private void downloadChunk() throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(downloadUrl).openConnection();

        if (supportsRangeRequests) {
            long endByte = Math.min(downloadedBytes + CHUNK_SIZE - 1, totalSize - 1);
            conn.setRequestProperty("Range", String.format("bytes=%d-%d", downloadedBytes, endByte));
        }

        try (InputStream in = conn.getInputStream();
             RandomAccessFile file = new RandomAccessFile(destinationPath.toFile(), "rw")) {

            file.seek(downloadedBytes);
            byte[] buffer = new byte[8192];
            int bytesRead;

            while ((bytesRead = in.read(buffer)) != -1) {
                file.write(buffer, 0, bytesRead);
                downloadedBytes += bytesRead;
            }
        }
    }

    private void handleDownloadError(IOException e) throws IOException {
        int retryCount = 0;
        while (retryCount < MAX_RETRIES) {
            try {
                Thread.sleep(RETRY_DELAY_MS);
                downloadChunk();
                return;
            } catch (IOException | InterruptedException retryException) {
                retryCount++;
            }
        }
        throw new IOException("Download failed after multiple retries", e);
    }

    private void verifyDownload() throws IOException {
        String actualHash = calculateFileHash(destinationPath);
        if (!expectedHash.equals(actualHash)) {
            throw new IOException("Download verification failed: Hash mismatch");
        }
    }

    private String calculateFileHash(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(Files.readAllBytes(file));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("Hash calculation failed", e);
        }
    }
}