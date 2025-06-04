package com.accurx.reliabledownloader.impl;

import com.accurx.reliabledownloader.core.AbstractDownloader;
import com.accurx.reliabledownloader.core.DownloaderConfig;
import com.accurx.reliabledownloader.core.FileDownloader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Optional;

public class ReliableDownloader extends AbstractDownloader implements FileDownloader {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReliableDownloader.class);

    private final DownloaderConfig config;

    public ReliableDownloader(DownloaderConfig config) {
        this.config = config;
    }

    private static class DownloadInitializationResult {
        final long totalSize;
        final boolean supportsRangeRequests;

        DownloadInitializationResult(long totalSize, boolean supportsRangeRequests) {
            this.totalSize = totalSize;
            this.supportsRangeRequests = supportsRangeRequests;
        }

        public long totalSize() {
            return totalSize;
        }

        public boolean supportsRangeRequests() {
            return supportsRangeRequests;
        }
    }

    @Override
    protected void beforeDownload() {
        // No specific setup needed before download starts in this implementation
    }

    @Override
    protected void afterDownload() {
        // No specific cleanup needed after download finishes in this implementation
    }

    @Override
    public Optional<String> performDownload(URI contentFileUrl, OutputStream destination) throws Exception {
        long totalSize = -1; // Overall total size
        boolean supportsRangeRequests = false;
        long currentDownloadedBytes = 0; // Cumulative downloaded bytes for the entire file

        try {
            // Step 1: Initialize download (get total size, check range support)
            DownloadInitializationResult initResult = initializeDownload(contentFileUrl);
            totalSize = initResult.totalSize();
            supportsRangeRequests = initResult.supportsRangeRequests();

            ByteArrayOutputStream md5Buffer = new ByteArrayOutputStream();
            MultiOutputStream multiDestination = new MultiOutputStream(destination, md5Buffer);

            // Step 2: Download chunks
            // Loop until all bytes are downloaded
            while (currentDownloadedBytes < totalSize) {
                // Pass the current offset and get the bytes downloaded in this chunk
                long bytesInThisChunk = downloadChunk(contentFileUrl, multiDestination,
                        currentDownloadedBytes, totalSize, supportsRangeRequests);

                // Add the bytes downloaded in this chunk to the cumulative total
                currentDownloadedBytes += bytesInThisChunk;

                // Notify progress *after* updating the cumulative downloaded bytes
                notifyProgress(currentDownloadedBytes, totalSize);
            }

            // Step 3: Verify hash if configured
            if (config.isVerifyHash()) {
                String contentMd5 = calculateMd5(md5Buffer.toByteArray());
                return Optional.of(contentMd5);
            }

            return Optional.empty();
        } catch (Exception e) {
            LOGGER.error("Download failed: {}", e.getMessage());
            throw e;
        }
    }

    private DownloadInitializationResult initializeDownload(URI contentFileUrl) throws IOException {
        int currentRetry = 0;
        IOException lastException = null;
        while (currentRetry <= config.getMaxRetries()) {
            HttpURLConnection conn = null;
            try {
                conn = (HttpURLConnection) contentFileUrl.toURL().openConnection();
                conn.setConnectTimeout((int) config.getConnectTimeout().toMillis());
                conn.setReadTimeout((int) config.getReadTimeout().toMillis());
                conn.setRequestMethod("HEAD");
                conn.connect();

                int responseCode = conn.getResponseCode();
                if (responseCode >= HttpURLConnection.HTTP_BAD_REQUEST) {
                    String responseMessage = conn.getResponseMessage();
                    if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                        throw new FileNotFoundException("Resource not found: " + contentFileUrl);
                    } else if (responseCode == HttpURLConnection.HTTP_FORBIDDEN) {
                        throw new SecurityException("Access forbidden: " + contentFileUrl);
                    } else {
                        throw new IOException("HTTP error: " + responseCode + " " + responseMessage + " for " + contentFileUrl);
                    }
                }

                String acceptRanges = conn.getHeaderField("Accept-Ranges");
                boolean supportsRangeRequests = config.isResumeSupport() &&
                        acceptRanges != null &&
                        acceptRanges.equals("bytes");

                long totalSize = conn.getContentLengthLong();
                if (totalSize == -1) {
                    throw new IOException("Could not determine file size from HEAD request for " + contentFileUrl);
                }

                LOGGER.info("Download size: {} bytes, Resume support: {}", totalSize, supportsRangeRequests);
                return new DownloadInitializationResult(totalSize, supportsRangeRequests);
            } catch (IOException e) {
                lastException = e;
                LOGGER.warn("Failed to initialize download for {}. Retrying {}/{}: {}",
                        contentFileUrl, currentRetry + 1, config.getMaxRetries(), e.getMessage());
                try {
                    Thread.sleep(config.getRetryDelay().toMillis());
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Initialization interrupted during retry delay.", ie);
                }
                currentRetry++;
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }
        // If we reach here, all retries failed
        throw new IOException("Failed to initialize download after " + config.getMaxRetries() + " retries.", lastException);
    }

    private long downloadChunk(URI contentFileUrl, OutputStream destination,
                               long currentOffset, long totalSize, boolean supportsRangeRequests) throws IOException {
        HttpURLConnection conn = null;
        long bytesReadInChunk = 0;
        try {
            conn = (HttpURLConnection) contentFileUrl.toURL().openConnection();
            conn.setConnectTimeout((int) config.getConnectTimeout().toMillis());
            conn.setReadTimeout((int) config.getReadTimeout().toMillis());

            if (supportsRangeRequests) {
                long endByte = Math.min(currentOffset + config.getChunkSize() - 1, totalSize - 1);
                conn.setRequestProperty("Range", String.format("bytes=%d-%d", currentOffset, endByte));
                LOGGER.debug("Downloading chunk: {} to {}", currentOffset, endByte);
            }

            int responseCode = conn.getResponseCode();
            if (responseCode >= HttpURLConnection.HTTP_BAD_REQUEST) {
                String responseMessage = conn.getResponseMessage();
                if (responseCode == HttpURLConnection.HTTP_NOT_FOUND) {
                    throw new FileNotFoundException("Resource not found during chunk download: " + contentFileUrl);
                } else if (responseCode == HttpURLConnection.HTTP_FORBIDDEN) {
                    throw new SecurityException("Access forbidden during chunk download: " + contentFileUrl);
                } else if (responseCode == HttpURLConnection.HTTP_PARTIAL && !supportsRangeRequests) {
                    LOGGER.warn("Received partial content without requesting range for {}", contentFileUrl);
                } else if (responseCode != HttpURLConnection.HTTP_OK && responseCode != HttpURLConnection.HTTP_PARTIAL) {
                    throw new IOException(
                            "HTTP error during chunk download: " + responseCode + " " + responseMessage + " for " + contentFileUrl);
                }
            }

            try (InputStream in = conn.getInputStream()) {
                byte[] buffer = new byte[config.getBufferSize()];
                int bytesRead;

                while ((bytesRead = in.read(buffer)) != -1) {
                    destination.write(buffer, 0, bytesRead);
                    bytesReadInChunk += bytesRead; // Accumulate bytes read in this chunk

                    if (bytesReadInChunk >= config.getChunkSize()) {
                        break;
                    }
                }
            }
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return bytesReadInChunk; // Return how many bytes were downloaded in this chunk
    }

    private String calculateMd5(byte[] data) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hash = digest.digest(data);
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("MD5 calculation failed", e);
        }
    }

    // Helper class to write to multiple output streams simultaneously
    private static class MultiOutputStream extends OutputStream {
        private final OutputStream[] outputs;

        public MultiOutputStream(OutputStream... outputs) {
            this.outputs = outputs;
        }

        @Override
        public void write(int b) throws IOException {
            for (OutputStream output : outputs) {
                output.write(b);
            }
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            for (OutputStream output : outputs) {
                output.write(b, off, len);
            }
        }

        @Override
        public void flush() throws IOException {
            for (OutputStream output : outputs) {
                output.flush();
            }
        }

        @Override
        public void close() throws IOException {
            // It's generally not advisable to close external streams passed in.
            // MultiOutputStream should not own the lifecycle of `destination` and `md5Buffer`
            // if they are managed by the caller.
            // However, the original code had this, so keeping it for consistency,
            // but noting that `FileDownloadCommand` already handles closing `destination`.
            for (OutputStream output : outputs) {
                output.close();
            }
        }
    }
}