package com.accurx.reliabledownloader.impl;

import com.accurx.reliabledownloader.core.AbstractDownloader;
import com.accurx.reliabledownloader.core.FileDownloader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.function.Supplier;

public class HTTPClientFileDownloader extends AbstractDownloader implements FileDownloader {
    private static final Logger LOGGER = LoggerFactory.getLogger(HTTPClientFileDownloader.class);
    private static final String ACCEPT_RANGES_HEADER = "Accept-Ranges";
    private static final String CONTENT_MD5 = "Content-MD5";
    private static final int BUFFER_SIZE = 8192;

    private final Supplier<HttpClient> httpClientSupplier;

    /**
     * Creates a new FileDownloaderImpl with the given HttpClient.Builder
     * @deprecated Use FileDownloaderImpl(Supplier<HttpClient>) instead
     */
    @Deprecated
    public HTTPClientFileDownloader(HttpClient.Builder httpClientBuilder) {
        this(() -> httpClientBuilder.build());
    }
    
    /**
     * Creates a new FileDownloaderImpl with the given HttpClient supplier
     * @param httpClientSupplier Supplier that provides HttpClient instances
     */
    public HTTPClientFileDownloader(Supplier<HttpClient> httpClientSupplier) {
        this.httpClientSupplier = httpClientSupplier;
    }

    @Override
    protected void beforeDownload() {

    }

    @Override
    protected void afterDownload() {

    }

    @Override
    public Optional<String> performDownload(URI contentFileUrl, OutputStream destination, long startOffset) throws IOException {
        try {
            var headResponse = sendHeadRequest(contentFileUrl);
            validateResponse(headResponse, "HEAD");

            boolean supportsRanges = headResponse.headers()
                    .allValues(ACCEPT_RANGES_HEADER)
                    .contains("bytes");

            if (supportsRanges) {
                LOGGER.info("Server supports range requests");
            }

            var getResponse = sendGetRequest(contentFileUrl);
            validateResponse(getResponse, "GET");

            transferContent(getResponse, destination);

            return getResponse.headers().firstValue(CONTENT_MD5);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Download interrupted", e);
        }
    }

    private HttpResponse<Void> sendHeadRequest(URI contentFileUrl) throws IOException, InterruptedException {
        var request = HttpRequest.newBuilder()
                .uri(contentFileUrl)
                .method("HEAD", HttpRequest.BodyPublishers.noBody())
                .build();
        return httpClientSupplier.get().send(request, HttpResponse.BodyHandlers.discarding());
    }

    private HttpResponse<InputStream> sendGetRequest(URI contentFileUrl) throws IOException, InterruptedException {
        var request = HttpRequest.newBuilder()
                .uri(contentFileUrl)
                .GET()
                .build();
        return httpClientSupplier.get().send(request, HttpResponse.BodyHandlers.ofInputStream());
    }

    private void validateResponse(HttpResponse<?> response, String requestType) throws IOException {
        if (response.statusCode() < 200 || response.statusCode() > 299) {
            throw new IOException(String.format("%s request failed with status code: %d",
                    requestType, response.statusCode()));
        }
    }

    private void transferContent(HttpResponse<InputStream> response, OutputStream destination) throws IOException {
        long totalBytes = response.headers()
                .firstValueAsLong("Content-Length")
                .orElse(-1L);
        long bytesTransferred = 0;

        try (var inputStream = response.body()) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                destination.write(buffer, 0, bytesRead);
                bytesTransferred += bytesRead;
                notifyProgress(bytesTransferred, totalBytes);
            }
            destination.flush();
            notifyComplete();
        } catch (IOException e) {
            notifyError(e);
            throw e;
        }
    }
}