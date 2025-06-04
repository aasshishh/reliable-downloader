package com.accurx.reliabledownloader.core;

import java.net.URI;
import java.nio.file.Path;

public class FileDownloadSettingsBuilder {
    private URI sourceUrl;
    private Path destinationPath = Path.of("./downloads/");
    private Boolean reliableDownloader = true;

    public FileDownloadSettingsBuilder withSource(URI url) {
        this.sourceUrl = url;
        return this;
    }

    public FileDownloadSettingsBuilder withDestination(Path path) {
        this.destinationPath = path;
        return this;
    }

    public FileDownloadSettingsBuilder withReliableDownloader(Boolean reliableDownloader) {
        this.reliableDownloader = reliableDownloader;
        return this;
    }

    public FileDownloadSettings build() {
        return new FileDownloadSettings(sourceUrl, destinationPath, reliableDownloader);
    }
}
