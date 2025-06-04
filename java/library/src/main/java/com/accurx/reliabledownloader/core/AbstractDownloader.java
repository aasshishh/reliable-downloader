package com.accurx.reliabledownloader.core;

import com.accurx.reliabledownloader.util.DownloadProgressObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public abstract class AbstractDownloader implements FileDownloader {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractDownloader.class);
    private final List<DownloadProgressObserver> observers = new ArrayList<>();

    protected abstract void beforeDownload();
    protected abstract void afterDownload();

    // Updated to include startOffset
    protected abstract Optional<String> performDownload(URI source, OutputStream destination, long startOffset) throws Exception;

    @Override
    public final Optional<String> downloadFile(URI source, OutputStream destination, long startOffset) throws Exception {
        beforeDownload();
        try {
            Optional<String> result = performDownload(source, destination, startOffset);
            notifyComplete();
            return result;
        } catch (IOException e) {
            // Notify observers about the error
            notifyError(e);
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            notifyError(new IOException("Download interrupted", e));
            throw new IOException("Download interrupted", e);
        } catch (Exception e) { // Catch any other unexpected exceptions
            LOGGER.error("An unexpected error occurred during download", e);
            notifyError(new IOException("Unexpected error during download", e));
            throw new IOException("Unexpected error during download", e);
        } finally {
            afterDownload();
        }
    }

    public void addObserver(DownloadProgressObserver observer) {
        observers.add(observer);
    }

    // Helper methods to notify observers
    protected void notifyProgress(long bytes, long total) {
        observers.forEach(o -> {
            try {
                o.onProgressUpdate(bytes, total);
            } catch (Exception e) {
                LOGGER.warn("Observer threw exception", e);
            }
        });
    }

    protected void notifyComplete() {
        observers.forEach(o -> {
            try {
                o.onComplete();
            } catch (Exception e) {
                LOGGER.warn("Observer threw exception", e);
            }
        });
    }

    protected void notifyError(Exception e) {
        observers.forEach(o -> {
            try {
                o.onError(e);
            } catch (Exception ex) {
                LOGGER.warn("Observer threw exception", ex);
            }
        });
    }
}
