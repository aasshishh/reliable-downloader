package com.accurx.reliabledownloader.impl;

import com.accurx.reliabledownloader.core.FileDownloader;
import com.accurx.reliabledownloader.util.DownloadProgressObserver;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.Optional;

public class ProgressTrackingDownloader implements FileDownloader {
    private final FileDownloader delegate;
    private final DownloadProgressObserver progressObserver;

    public ProgressTrackingDownloader(FileDownloader delegate) {
        this.delegate = delegate;
        this.progressObserver = new ConsoleProgressObserver();
    }

    @Override
    public Optional<String> downloadFile(URI source, OutputStream destination) throws Exception {
        // Since we don't have actual progress information, we'll simulate it with initial progress
        progressObserver.onProgressUpdate(0, 100);
        
        try {
            Optional<String> result = delegate.downloadFile(source, destination);
            // Mark as 100% complete
            progressObserver.onProgressUpdate(100, 100);
            progressObserver.onComplete();
            return result;
        } catch (Exception e) {
            progressObserver.onError(e);
            throw e;
        }
    }
}
