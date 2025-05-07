package com.accurx.reliabledownloader;

import java.net.URI;
import java.nio.file.Path;

public record FileDownloadSettings(
        URI SourceUrl,
        Path DestinationFilePath
) {}
