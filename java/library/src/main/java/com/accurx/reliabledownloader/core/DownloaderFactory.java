package com.accurx.reliabledownloader.core;

import com.accurx.reliabledownloader.impl.HTTPClientFileDownloader;
import com.accurx.reliabledownloader.impl.ProgressTrackingDownloader;
import com.accurx.reliabledownloader.impl.ReliableDownloader;
import com.accurx.reliabledownloader.impl.RetryingDownloader;

import java.net.http.HttpClient;
import java.util.function.Supplier;

public class DownloaderFactory {

    public FileDownloader createDownloader(DownloaderConfig config, FileDownloadSettings settings) {
        if (settings.downloader().equals("ReliableDownloader")) {
            return createReliableDownloader(config);
        }
        return createHTTPClientFileDownloader(config);
    }

    public FileDownloader createHTTPClientFileDownloader(DownloaderConfig config) {
        FileDownloader base = new HTTPClientFileDownloader(
                (Supplier<HttpClient>) HttpClient.newBuilder()
                        .connectTimeout(config.getConnectTimeout())
                        .build());

        if (config.isResumeSupport()) {
            base = new RetryingDownloader(base, config.getMaxRetries(), config.getRetryDelay());
        }

        if (config.isProgressTrackingEnabled()) {
            base = new ProgressTrackingDownloader(base);
        }

        return base;
    }

    public FileDownloader createReliableDownloader(DownloaderConfig config) {
        FileDownloader base = new ReliableDownloader(config);

        if (config.isResumeSupport()) {
            base = new RetryingDownloader(base, config.getMaxRetries(), config.getRetryDelay());
        }

        if (config.isProgressTrackingEnabled()) {
            base = new ProgressTrackingDownloader(base);
        }

        return base;
    }
}
