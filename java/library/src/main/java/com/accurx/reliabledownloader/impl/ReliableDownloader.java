package com.accurx.reliabledownloader.impl;

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

public class ReliableDownloader implements FileDownloader {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReliableDownloader.class);

    private final DownloaderConfig config;
    private boolean supportsRangeRequests = false;
    private long totalSize = -1;
    private long downloadedBytes = 0;

    public ReliableDownloader(DownloaderConfig config) {
        this.config = config;
    }

    @Override
    public Optional<String> downloadFile(URI contentFileUrl, OutputStream destination) throws Exception {
        try {
            initializeDownload(contentFileUrl);
            ByteArrayOutputStream md5Buffer = new ByteArrayOutputStream();

            while (downloadedBytes < totalSize) {
                try {
                    downloadChunk(contentFileUrl, new MultiOutputStream(destination, md5Buffer));
                } catch (IOException e) {
                    handleDownloadError(e, contentFileUrl, destination);
                }
            }

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

    private void initializeDownload(URI contentFileUrl) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) contentFileUrl.toURL().openConnection();
        conn.setConnectTimeout((int) config.getConnectTimeout().toMillis());
        conn.setReadTimeout((int) config.getReadTimeout().toMillis());
        conn.setRequestMethod("HEAD");
        conn.connect();

        // Check if server supports range requests
        String acceptRanges = conn.getHeaderField("Accept-Ranges");
        this.supportsRangeRequests = config.isResumeSupport() &&
                acceptRanges != null &&
                acceptRanges.equals("bytes");

        totalSize = conn.getContentLengthLong();
        conn.disconnect();

        if (totalSize == -1) {
            throw new IOException("Could not determine file size");
        }

        LOGGER.info("Download size: {} bytes, Resume support: {}", totalSize, supportsRangeRequests);
    }

    private void downloadChunk(URI contentFileUrl, OutputStream destination) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) contentFileUrl.toURL().openConnection();
        conn.setConnectTimeout((int) config.getConnectTimeout().toMillis());
        conn.setReadTimeout((int) config.getReadTimeout().toMillis());

        if (supportsRangeRequests) {
            long endByte = Math.min(downloadedBytes + config.getChunkSize() - 1, totalSize - 1);
            conn.setRequestProperty("Range", String.format("bytes=%d-%d", downloadedBytes, endByte));
            LOGGER.debug("Downloading chunk: {} to {}", downloadedBytes, endByte);
        }

        try (InputStream in = conn.getInputStream()) {
            byte[] buffer = new byte[config.getBufferSize()];
            int bytesRead;
            long chunkBytesRead = 0;

            while ((bytesRead = in.read(buffer)) != -1) {
                destination.write(buffer, 0, bytesRead);
                downloadedBytes += bytesRead;
                chunkBytesRead += bytesRead;

                if (chunkBytesRead >= config.getChunkSize()) {
                    break;
                }
            }
        }
    }

    private void handleDownloadError(IOException e, URI contentFileUrl, OutputStream destination) throws IOException {
        int retryCount = 0;
        while (retryCount < config.getMaxRetries()) {
            try {
                LOGGER.warn("Download error, retrying ({}/{}): {}",
                        retryCount + 1, config.getMaxRetries(), e.getMessage());
                Thread.sleep(config.getRetryDelay().toMillis());
                downloadChunk(contentFileUrl, destination);
                return;
            } catch (IOException | InterruptedException retryException) {
                retryCount++;
                if (retryCount == config.getMaxRetries()) {
                    throw new IOException("Download failed after " + retryCount + " retries", e);
                }
            }
        }
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
            for (OutputStream output : outputs) {
                output.close();
            }
        }
    }
}