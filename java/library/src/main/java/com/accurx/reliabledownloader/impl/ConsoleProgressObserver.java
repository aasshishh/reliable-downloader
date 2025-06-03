package com.accurx.reliabledownloader.impl;

import com.accurx.reliabledownloader.util.DownloadProgressObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConsoleProgressObserver implements DownloadProgressObserver {
    private static final Logger logger = LoggerFactory.getLogger(ConsoleProgressObserver.class);

    @Override
    public void onProgressUpdate(long bytesDownloaded, long totalBytes) {
        double progress = (double) bytesDownloaded / totalBytes * 100;
        logger.info("Download progress: {:.2f}%", progress);
    }

    @Override
    public void onComplete() {
        logger.info("Download completed");
    }

    @Override
    public void onError(Exception e) {
        logger.error("Download error: {}", e.getMessage());
    }
}

