package com.accurx.reliabledownloader.core;

import com.accurx.reliabledownloader.util.DownloadProgressObserver;

import java.io.OutputStream;
import java.net.URI;
import java.util.Optional;

public interface FileDownloader {
    /**
     * Downloads a file, trying to use reliable downloading if possible
     * @param contentFileUrl The url which the file is hosted at
     * @param destination The stream to write the file contents to
     * @param startOffset The offset from which to start the download (for resuming)
     * @return the MD5 checksum if present (b64 encoded)
     */
    Optional<String> downloadFile(URI contentFileUrl, OutputStream destination, long startOffset) throws Exception;

    default void addObserver(DownloadProgressObserver observer) {}
}

