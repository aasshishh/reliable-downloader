package com.accurx.reliabledownloader.core;

import java.net.URI;
import java.nio.file.Path;

public record FileDownloadSettings(
        URI sourceUrl,
        Path destinationFilePath,
        Boolean reliableDownloader
) {}
