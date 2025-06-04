package com.accurx.reliabledownloader.impl;

import com.accurx.reliabledownloader.core.AbstractDownloader;
import com.accurx.reliabledownloader.core.DownloaderConfig;
import com.accurx.reliabledownloader.core.FileDownloader;
import com.accurx.reliabledownloader.core.RangeNotSupportedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
    public Optional<String> performDownload(URI contentFileUrl, OutputStream destination, long startOffset) throws Exception {
        HttpURLConnection connection = null;
        Optional<String> downloadedDataMd5 = Optional.empty();

        long totalSize = -1; // Overall total size
        boolean supportsRangeRequests = false;
        long currentDownloadedBytes = startOffset; // Initialize with startOffset for resuming

        try {
            // Step 1: Initialize download (get total size, check range support)
            // Pass the current offset to initializeDownload to verify consistency with server
            DownloadInitializationResult initResult = initializeDownload(contentFileUrl, startOffset);
            totalSize = initResult.totalSize();
            supportsRangeRequests = initResult.supportsRangeRequests();

            // If the server doesn't support range requests, we must start from 0 regardless of startOffset
            if (!supportsRangeRequests && startOffset > 0) {
                LOGGER.warn("Server does not support range requests. Full file needs to be re-downloaded. " +
                        "Throwing RangeNotSupportedException");
                throw new RangeNotSupportedException();
            }
        } catch (Exception e) {
            LOGGER.error("Download Initialization failed: {}", e.getMessage());
            throw e;
        }

        try {

            // If the file is already fully downloaded based on the initial check
            if (currentDownloadedBytes == totalSize && totalSize != -1) {
                LOGGER.info("File already fully downloaded. Skipping download process.");
            }

            ByteArrayOutputStream md5Buffer = null;
            if (startOffset == 0 && config.isVerifyHash()) {
                md5Buffer = new ByteArrayOutputStream();
            }
            OutputStream finalDestination;
            if (md5Buffer != null) {
                finalDestination = new MultiOutputStream(destination, md5Buffer);
            } else {
                finalDestination = destination;
            }

            // Notify initial progress with the existing downloaded bytes
            notifyProgress(currentDownloadedBytes, totalSize);

            // Step 2: Download chunks
            // Loop until all bytes are downloaded
            while (currentDownloadedBytes < totalSize) {
                // Pass the current offset and get the bytes downloaded in this chunk
                long bytesInThisChunk = downloadChunk(contentFileUrl, finalDestination,
                        currentDownloadedBytes, totalSize, supportsRangeRequests);

                // Add the bytes downloaded in this chunk to the cumulative total
                currentDownloadedBytes += bytesInThisChunk;

                // Notify progress *after* updating the cumulative downloaded bytes
                notifyProgress(currentDownloadedBytes, totalSize);
            }

            // Step 3: Verify hash if configured
            if (config.isVerifyHash() && md5Buffer != null) {
                String contentMd5 = calculateMd5(md5Buffer.toByteArray());
                return Optional.of(contentMd5);
            }

            return Optional.empty();
        } catch (Exception e) {
            LOGGER.error("Download failed: {}", e.getMessage());
            throw e;
        }
    }

    private DownloadInitializationResult initializeDownload(URI contentFileUrl, long currentOffset) throws IOException {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) contentFileUrl.toURL().openConnection();
            conn.setConnectTimeout((int) config.getConnectTimeout().toMillis());
            conn.setReadTimeout((int) config.getReadTimeout().toMillis());
            conn.setRequestMethod("HEAD");

            // If resuming, send a Range header even for the HEAD request to check consistency
            if (config.isResumeSupport() && currentOffset > 0) {
                conn.setRequestProperty("Range", "bytes=" + currentOffset + "-");
                LOGGER.debug("Sending HEAD request with Range header: bytes={}-", currentOffset);
            }

            conn.connect();

            int responseCode = conn.getResponseCode();
            // Handle 416 specifically for HEAD requests if range was requested but not satisfiable
            if (responseCode == 416 && currentOffset > 0) {
                LOGGER.warn("Server returned 416 (Range Not Satisfiable) for HEAD request. " +
                        "This might mean the local partial file is larger than the remote file, " +
                        "or the file has changed. Restarting download from 0.");
                currentOffset = 0; // Reset offset, force full download
                // Re-try HEAD request without Range header to get correct total size
                conn.disconnect(); // Disconnect current connection
                conn = (HttpURLConnection) contentFileUrl.toURL().openConnection();
                conn.setConnectTimeout((int) config.getConnectTimeout().toMillis());
                conn.setReadTimeout((int) config.getReadTimeout().toMillis());
                conn.setRequestMethod("HEAD");
                conn.connect();
                responseCode = conn.getResponseCode(); // Get new response code
            }

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
            // Resume support is only true if config enables it AND server advertises it
            boolean supportsRangeRequests = config.isResumeSupport() &&
                    acceptRanges != null &&
                    acceptRanges.equals("bytes");

            long totalSize = conn.getContentLengthLong();

            String contentRangeHeader = conn.getHeaderField("Content-Range");
            if (contentRangeHeader != null) {
                try {
                    // Example: bytes 0-100/200 -> total size is 200
                    int slashIndex = contentRangeHeader.indexOf('/');
                    if (slashIndex != -1) {
                        totalSize = Long.parseLong(contentRangeHeader.substring(slashIndex + 1));
                    }
                } catch (NumberFormatException e) {
                    LOGGER.warn("Could not parse Content-Range header for total size: {}", contentRangeHeader);
                }
            }


            if (totalSize == -1) {
                LOGGER.warn("Could not determine total file size from HEAD request for {}. Attempting partial GET.", contentFileUrl);
                HttpURLConnection getConn = null; // Corrected: new connection for GET
                try {
                    getConn = (HttpURLConnection) contentFileUrl.toURL().openConnection();
                    getConn.setConnectTimeout((int) config.getConnectTimeout().toMillis());
                    getConn.setReadTimeout((int) config.getReadTimeout().toMillis());
                    getConn.setRequestMethod("GET");
                    // Request a small range to avoid downloading the whole file
                    getConn.setRequestProperty("Range", "bytes=0-0"); // Request just 1 byte
                    getConn.connect();

                    if (getConn.getResponseCode() == HttpURLConnection.HTTP_PARTIAL || getConn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                        long potentialTotal = getConn.getContentLengthLong();
                        if (potentialTotal != -1) {
                            // If we got a partial content length, it's just for the 1 byte, so not useful here.
                            // We need the Content-Range header for total size.
                            String getResponseContentRange = getConn.getHeaderField("Content-Range");
                            if (getResponseContentRange != null) {
                                int slashIndex = getResponseContentRange.indexOf('/');
                                if (slashIndex != -1) {
                                    totalSize = Long.parseLong(getResponseContentRange.substring(slashIndex + 1));
                                }
                            }
                        }
                    }
                } catch (Exception ex) {
                    LOGGER.warn("Failed to get total size from partial GET: {}", ex.getMessage());
                } finally {
                    if (getConn != null) {
                        getConn.disconnect();
                    }
                }
                if (totalSize == -1) {
                    throw new IOException("Could not determine file size for " + contentFileUrl);
                }
            }

            // If resuming, check if the currentOffset is valid against the total size
            if (config.isResumeSupport() && currentOffset > 0) {
                if (currentOffset > totalSize) {
                    LOGGER.warn("Local partial file size ({}) is greater than remote total size ({}). Restarting download from 0.", currentOffset, totalSize);
                    currentOffset = 0; // Force restart if local file is larger than remote
                }
            }


            LOGGER.info("Download size: {} bytes, Resume support: {}", totalSize, supportsRangeRequests);
            return new DownloadInitializationResult(totalSize, supportsRangeRequests);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private long downloadChunk(URI contentFileUrl, OutputStream destination,
                               long currentOffset, long totalSize, boolean supportsRangeRequests) throws IOException {
        HttpURLConnection conn = null;
        long bytesReadInChunk = 0;
        try {
            conn = (HttpURLConnection) contentFileUrl.toURL().openConnection();
            conn.setConnectTimeout((int) config.getConnectTimeout().toMillis());
            conn.setReadTimeout((int) config.getReadTimeout().toMillis());

            // If range requests are supported and we're resuming, set the Range header
            if (supportsRangeRequests) {
                long endByte = Math.min(currentOffset + config.getChunkSize() - 1, totalSize - 1);
                conn.setRequestProperty("Range", String.format("bytes=%d-%d", currentOffset, endByte));
                LOGGER.debug("Downloading chunk: {} to {}", currentOffset, endByte);
            } else {
                // If not supporting range requests, ensure we always start from 0.
                if (currentOffset > 0) {
                    LOGGER.warn("Server does not support range requests, but an offset was requested. " +
                            "This chunk download will likely start from the beginning of the file.");
                }
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
                } else if (responseCode == HttpURLConnection.HTTP_OK && supportsRangeRequests && currentOffset > 0) {
                    LOGGER.warn("Server ignored Range header and returned full content (HTTP 200 OK) for {}. " +
                            "Starting download from the beginning of the stream.", contentFileUrl);
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

                    // If we're not supporting ranges, and we read more than a chunk size,
                    // we might be reading the full file. Break if we've read enough for a chunk.
                    // Otherwise, continue reading until the stream ends.
                    if (supportsRangeRequests && bytesReadInChunk >= config.getChunkSize()) {
                        break;
                    }
                    // If not supportsRangeRequests, read until stream ends, and bytesReadInChunk will be the full amount read for this "chunk"
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