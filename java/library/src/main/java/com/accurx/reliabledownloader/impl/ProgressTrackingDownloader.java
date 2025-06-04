package com.accurx.reliabledownloader.impl;

import com.accurx.reliabledownloader.core.AbstractDownloader;
import com.accurx.reliabledownloader.core.FileDownloader;
import com.accurx.reliabledownloader.util.DownloadProgressObserver;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.Optional;

public class ProgressTrackingDownloader implements FileDownloader, DownloadProgressObserver { // Implement DownloadProgressObserver
    private final FileDownloader delegate;
    private final DownloadProgressObserver externalProgressObserver;

    public ProgressTrackingDownloader(FileDownloader delegate, DownloadProgressObserver externalProgressObserver) {
        this.delegate = delegate;
        this.externalProgressObserver = externalProgressObserver;

        delegate.addObserver(this);
    }

    @Override
    public Optional<String> downloadFile(URI source, OutputStream destination) throws Exception {
        // The initial 0% update is good for immediate feedback
        externalProgressObserver.onProgressUpdate(0, 100);

        try {
            Optional<String> result = delegate.downloadFile(source, destination);
            // The final 100% update ensures completion is always shown
            externalProgressObserver.onProgressUpdate(100, 100);
            externalProgressObserver.onComplete();
            return result;
        } catch (Exception e) {
            externalProgressObserver.onError(e);
            throw e;
        }
    }

    @Override
    public void addObserver(DownloadProgressObserver observer) {
        delegate.addObserver(observer);
    }


    // --- DownloadProgressObserver methods (This ProgressTrackingDownloader *is* an observer) ---
    @Override
    public void onProgressUpdate(long bytesDownloaded, long totalBytes) {
        // Forward the progress updates received from the delegate to the external observer
        externalProgressObserver.onProgressUpdate(bytesDownloaded, totalBytes);
    }

    @Override
    public void onComplete() {
        // We handle onComplete within downloadFile, so this can be empty or log if needed
    }

    @Override
    public void onError(Exception e) {
        // We handle onError within downloadFile, so this can be empty or log if needed
    }
}

