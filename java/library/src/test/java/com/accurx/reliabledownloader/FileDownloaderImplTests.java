package com.accurx.reliabledownloader;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentCaptor;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.util.concurrent.CompletableFuture;
import javax.net.ssl.SSLSession;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Builder;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileDownloaderImplTest {
    private HttpClient httpClient;
    private FileDownloaderImpl fileDownloader;
    private static final URI TEST_URI = URI.create("http://example.com/test.file");

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        // Create a supplier that returns our test HttpClient
        fileDownloader = new FileDownloaderImpl(() -> httpClient);
    }

    @Test
    void successfulDownload() throws Exception {
        // Arrange
        String testContent = "Test content";
        String expectedMd5 = "test-md5-hash";
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // Setup TestHttpClient
        HttpResponse<Void> headResponse = setupHeadResponse(200, true);
        HttpResponse<InputStream> getResponse = setupGetResponse(200, testContent, expectedMd5);
        httpClient = new TestHttpClient(headResponse, getResponse);
        setUp(); // Re-initialize fileDownloader with the new httpClient

        // Act
        Optional<String> md5 = fileDownloader.downloadFile(TEST_URI, outputStream);

        // Assert
        assertTrue(md5.isPresent());
        assertEquals(expectedMd5, md5.get());
        assertEquals(testContent, outputStream.toString());
    }

    @Test
    void failedHeadRequest() throws Exception {
        // Arrange
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // Setup TestHttpClient
        HttpResponse<Void> headResponse = setupHeadResponse(404, false);
        HttpResponse<InputStream> getResponse = setupGetResponse(404, "", null);
        httpClient = new TestHttpClient(headResponse, getResponse);
        setUp(); // Re-initialize fileDownloader with the new httpClient

        // Act & Assert
        assertThrows(IOException.class, () ->
                fileDownloader.downloadFile(TEST_URI, outputStream)
        );
    }

    @Test
    void handleInterruptedException() throws Exception {
        // Arrange
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        
        // Setup TestHttpClient that throws InterruptedException
        httpClient = new HttpClient() {
            @Override
            public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler) throws IOException, InterruptedException {
                throw new InterruptedException("Test interruption");
            }

            @Override
            public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request,
                    HttpResponse.BodyHandler<T> responseBodyHandler,
                    HttpResponse.PushPromiseHandler<T> pushPromiseHandler) {
                throw new UnsupportedOperationException("Async operations not supported in tests");
            }
            
            @Override
            public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request,
                    HttpResponse.BodyHandler<T> responseBodyHandler) {
                throw new UnsupportedOperationException("Async operations not supported in tests");
            }

            public static Builder newBuilder() { return HttpClient.newBuilder(); }
            
            public Optional<CookieHandler> cookieHandler() { return Optional.empty(); }
            @Override
            public Optional<Duration> connectTimeout() { return Optional.empty(); }
            @Override
            public Redirect followRedirects() { return Redirect.NEVER; }
            @Override
            public Optional<ProxySelector> proxy() { return Optional.empty(); }
            @Override
            public SSLContext sslContext() { return null; }
            @Override
            public SSLParameters sslParameters() { return null; }
            @Override
            public Optional<Authenticator> authenticator() { return Optional.empty(); }
            @Override
            public Version version() { return Version.HTTP_1_1; }
            @Override
            public Optional<Executor> executor() { return Optional.empty(); }
        };
        setUp(); // Re-initialize fileDownloader with the new httpClient

        // Act & Assert
        assertThrows(IOException.class, () ->
                fileDownloader.downloadFile(TEST_URI, outputStream)
        );
        assertTrue(Thread.currentThread().isInterrupted());
    }

    private HttpResponse<Void> setupHeadResponse(int statusCode, boolean supportsRanges) {
        return new TestHttpResponse<>(
            statusCode,
            HttpHeaders.of(Map.of("Accept-Ranges", List.of(supportsRanges ? "bytes" : "none")),
                (k, v) -> true),
            null
        );
    }

    private HttpResponse<InputStream> setupGetResponse(int statusCode, String content, String md5) {
        return new TestHttpResponse<>(
            statusCode,
            HttpHeaders.of(Map.of("Content-MD5", List.of(md5 != null ? md5 : "")),
                (k, v) -> true),
            new ByteArrayInputStream(content.getBytes())
        );
    }

    // Test implementation of HttpClient
    private static class TestHttpClient extends HttpClient {
        private final HttpResponse<Void> headResponse;
        private final HttpResponse<InputStream> getResponse;

        public TestHttpClient(HttpResponse<Void> headResponse, HttpResponse<InputStream> getResponse) {
            this.headResponse = headResponse;
            this.getResponse = getResponse;
        }

        @Override
        public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> responseBodyHandler) throws IOException, InterruptedException {
            if (responseBodyHandler.equals(HttpResponse.BodyHandlers.discarding())) {
                return (HttpResponse<T>) headResponse;
            } else if (responseBodyHandler.equals(HttpResponse.BodyHandlers.ofInputStream())) {
                return (HttpResponse<T>) getResponse;
            }
            throw new UnsupportedOperationException("Unexpected body handler");
        }

        @Override
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request,
                HttpResponse.BodyHandler<T> responseBodyHandler,
                HttpResponse.PushPromiseHandler<T> pushPromiseHandler) {
            throw new UnsupportedOperationException("Async operations not supported in tests");
        }
        
        @Override
        public <T> CompletableFuture<HttpResponse<T>> sendAsync(HttpRequest request,
                HttpResponse.BodyHandler<T> responseBodyHandler) {
            throw new UnsupportedOperationException("Async operations not supported in tests");
        }

        public static Builder newBuilder() { return HttpClient.newBuilder(); }
        
        public Optional<CookieHandler> cookieHandler() { return Optional.empty(); }
        @Override
        public Optional<Duration> connectTimeout() { return Optional.empty(); }
        @Override
        public Redirect followRedirects() { return Redirect.NEVER; }
        @Override
        public Optional<ProxySelector> proxy() { return Optional.empty(); }
        @Override
        public SSLContext sslContext() { return null; }
        @Override
        public SSLParameters sslParameters() { return null; }
        @Override
        public Optional<Authenticator> authenticator() { return Optional.empty(); }
        @Override
        public Version version() { return Version.HTTP_1_1; }
        @Override
        public Optional<Executor> executor() { return Optional.empty(); }
    }

    // Test implementation of HttpResponse
    private static class TestHttpResponse<T> implements HttpResponse<T> {
        private final int statusCode;
        private final HttpHeaders headers;
        private final T body;

        public TestHttpResponse(int statusCode, HttpHeaders headers, T body) {
            this.statusCode = statusCode;
            this.headers = headers;
            this.body = body;
        }

        @Override
        public int statusCode() {
            return statusCode;
        }

        @Override
        public HttpHeaders headers() {
            return headers;
        }

        @Override
        public T body() {
            return body;
        }

        @Override
        public Optional<HttpResponse<T>> previousResponse() {
            return Optional.empty();
        }

        @Override
        public HttpRequest request() {
            return HttpRequest.newBuilder().uri(URI.create("http://test")).build();
        }

        @Override
        public URI uri() {
            return URI.create("http://test");
        }

        @Override
        public Version version() {
            return Version.HTTP_1_1;
        }

        @Override
        public Optional<SSLSession> sslSession() {
            return Optional.empty();
        }
    }
}