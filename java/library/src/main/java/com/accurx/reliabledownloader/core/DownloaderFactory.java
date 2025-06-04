package com.accurx.reliabledownloader.core;

import com.accurx.reliabledownloader.impl.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpClient;
import java.util.function.Supplier;

public class DownloaderFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(DownloaderFactory.class);

    public FileDownloader createDownloader(DownloaderConfig config, FileDownloadSettings settings) {
        if (settings.downloader().equals("ReliableDownloader")) {
            LOGGER.info("Using reliable downloader");
            return createReliableDownloader(config);
        }
        LOGGER.info("Using HTTP client based downloader");
        return createHTTPClientFileDownloader(config);
    }

    public FileDownloader createHTTPClientFileDownloader(DownloaderConfig config) {
        FileDownloader base = new HTTPClientFileDownloader(
                () -> HttpClient.newBuilder()
                        .connectTimeout(config.getConnectTimeout())
                        .build());

        // Generally meant for Reliable Downloader, but can be used for other purposes as well.
        if (config.isResumeSupport()) {
            base = new RetryingDownloader(base, config.getMaxRetries(), config.getRetryDelay());
        }

        if (config.isProgressTrackingEnabled()) {
            base = new ProgressTrackingDownloader(base, new ConsoleProgressObserver());
        }

        return base;
    }

    public FileDownloader createReliableDownloader(DownloaderConfig config) {
        FileDownloader base = new ReliableDownloader(config);

        // Always retry on HTTP client errors, regardless of config settings.
        base = new RetryingDownloader(base, config.getMaxRetries(), config.getRetryDelay());

        if (config.isProgressTrackingEnabled()) {
            base = new ProgressTrackingDownloader(base, new ConsoleProgressObserver());
        }

        return base;
    }
}
