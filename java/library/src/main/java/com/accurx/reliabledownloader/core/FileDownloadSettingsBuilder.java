package com.accurx.reliabledownloader.core;

import java.net.URI;
import java.nio.file.Path;

public class FileDownloadSettingsBuilder {
    private URI sourceUrl;
    private Path destinationPath = Path.of("./downloads/");
    // private String downloader = "HTTPDownloader";
    private String downloader = "ReliableDownloader";

    public FileDownloadSettingsBuilder withSource(URI url) {
        this.sourceUrl = url;
        return this;
    }

    public FileDownloadSettingsBuilder withDestination(Path path) {
        this.destinationPath = path;
        return this;
    }

    public FileDownloadSettingsBuilder withDownloader(String downloader) {
        this.downloader = downloader;
        return this;
    }

    public FileDownloadSettings build() {
        return new FileDownloadSettings(sourceUrl, destinationPath, downloader);
    }
}
