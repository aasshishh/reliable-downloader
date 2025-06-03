package com.accurx.reliabledownloader.util;

public interface DownloadProgressObserver {
    void onProgressUpdate(long bytesDownloaded, long totalBytes);
    void onComplete();
    void onError(Exception e);
}