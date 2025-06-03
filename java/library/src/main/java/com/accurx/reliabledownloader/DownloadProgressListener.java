package com.accurx.reliabledownloader;

public interface DownloadProgressListener {
    void onProgress(long bytesDownloaded, long totalSize);
    void onComplete();
    void onError(Exception e);
}
