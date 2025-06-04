package com.accurx.reliabledownloader.impl;

import com.accurx.reliabledownloader.core.AbstractDownloader;
import com.accurx.reliabledownloader.core.FileDownloader;
import com.accurx.reliabledownloader.util.DownloadProgressObserver;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.time.Duration;
import java.util.Optional;

public class RetryingDownloader implements FileDownloader {
    private final FileDownloader delegate;
    private final int maxRetries;
    private final Duration retryDelay;

    public RetryingDownloader(FileDownloader delegate, int maxRetries, Duration retryDelay) {
        this.delegate = delegate;
        this.maxRetries = maxRetries;
        this.retryDelay = retryDelay;
    }

    @Override
    public Optional<String> downloadFile(URI source, OutputStream destination) throws Exception {
        int attempts = 0;
        while (attempts < maxRetries) {
            try {
                return delegate.downloadFile(source, destination);
            } catch (IOException e) {
                attempts++;
                if (attempts == maxRetries) throw e;
                Thread.sleep(calculateBackoff(attempts));
            }
        }
        throw new IOException("Download failed after " + maxRetries + " attempts");
    }

    /**
     * Calculates the backoff delay for retries using an exponential backoff strategy.
     * The delay increases with each attempt.
     * @param attempts The current number of retry attempts (1-based).
     * @return The delay in milliseconds.
     */
    private long calculateBackoff(int attempts) {
        long baseDelay = retryDelay.toMillis();
        // Exponential backoff: baseDelay * 2^(attempts - 1)
        return baseDelay * (long) Math.pow(2, attempts - 1);
    }

    @Override
    public void addObserver(DownloadProgressObserver observer) {
        // Delegate the observer addition to the wrapped FileDownloader
        delegate.addObserver(observer);
    }
}
