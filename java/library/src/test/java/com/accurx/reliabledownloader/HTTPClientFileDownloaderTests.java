package com.accurx.reliabledownloader.impl;

import com.accurx.reliabledownloader.util.DownloadProgressObserver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class) // Integrates Mockito with JUnit 5
class HTTPClientFileDownloaderTests {
    @Mock
    private HttpClient mockHttpClient;
    @Mock
    private HttpResponse<Void> mockHeadResponse;
    @Mock
    private HttpResponse<InputStream> mockGetResponse;
    @Mock
    private DownloadProgressObserver mockObserver;

    @Captor
    private ArgumentCaptor<Long> bytesDownloadedCaptor;
    @Captor
    private ArgumentCaptor<Long> totalBytesCaptor;
    @Captor
    private ArgumentCaptor<Exception> exceptionCaptor;

    private HTTPClientFileDownloader spyDownloader;
    private URI testUri;

    @BeforeEach
    void setUp() {
        // Create the downloader and spy on it
        HTTPClientFileDownloader downloader = new HTTPClientFileDownloader(() -> mockHttpClient);
        spyDownloader = spy(downloader);
        spyDownloader.addObserver(mockObserver);
        testUri = URI.create("http://example.com/testfile.txt");
    }

    @Test
    @DisplayName("should call beforeDownload and afterDownload hooks")
    void shouldCallHooks() throws Exception {
        String fileContent = "dummy";
        // Mock HEAD response
        when(mockHttpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.discarding())))
                .thenReturn(mockHeadResponse);
        when(mockHeadResponse.statusCode()).thenReturn(200);
        when(mockHeadResponse.headers()).thenReturn(HttpHeaders.of(Map.of(), (a, b) -> true));

        // Mock GET response
        when(mockHttpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofInputStream())))
                .thenReturn(mockGetResponse);
        when(mockGetResponse.statusCode()).thenReturn(200);
        when(mockGetResponse.headers()).thenReturn(HttpHeaders.of(
                Map.of("Content-Length",
                        List.of(String.valueOf(fileContent.length()))), (a, b) -> true));
        when(mockGetResponse.body()).thenReturn(new ByteArrayInputStream(fileContent.getBytes()));

        ByteArrayOutputStream destination = new ByteArrayOutputStream();
        spyDownloader.downloadFile(testUri, destination, 0);

        assertEquals(fileContent, destination.toString());
        verify(mockObserver, atLeastOnce()).onComplete();
        verify(mockObserver, never()).onError(any(Exception.class));
    }

    @Test
    @DisplayName("should successfully download a file and return MD5 if provided")
    void performDownload_success_returnsMd5() throws Exception {
        String fileContent = "This is a test file content.";
        String expectedMd5 = "Lq0f1pUq07Tf8p3Sg9p3Pg=="; // Base64 MD5 of "This is a test file content."

        // Mock HEAD response
        when(mockHttpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.discarding())))
                .thenReturn(mockHeadResponse);
        when(mockHeadResponse.statusCode()).thenReturn(200);
        when(mockHeadResponse.headers())
                .thenReturn(
                        HttpHeaders.of(Map.of("Accept-Ranges", List.of("bytes")), (a, b) -> true));

        // Mock GET response
        when(mockHttpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofInputStream())))
                .thenReturn(mockGetResponse);
        when(mockGetResponse.statusCode()).thenReturn(200);
        when(mockGetResponse.headers()).thenReturn(HttpHeaders.of(
                Map.of("Content-Length", List.of(String.valueOf(fileContent.length())),
                        "Content-MD5", List.of(expectedMd5)), (a, b) -> true));
        when(mockGetResponse.body()).thenReturn(new ByteArrayInputStream(fileContent.getBytes()));

        ByteArrayOutputStream destination = new ByteArrayOutputStream();
        Optional<String> actualMd5 = spyDownloader.performDownload(testUri, destination, 0); // Use spyDownloader

        assertTrue(actualMd5.isPresent());
        assertEquals(expectedMd5, actualMd5.get());
        assertEquals(fileContent, destination.toString());

        // Verify observer notifications
        verify(mockObserver, atLeastOnce())
                .onProgressUpdate(bytesDownloadedCaptor.capture(), totalBytesCaptor.capture());
        assertTrue(bytesDownloadedCaptor.getValue() > 0);
        assertEquals(fileContent.length(), totalBytesCaptor.getValue());
        verify(mockObserver).onComplete();
        verify(mockObserver, never()).onError(any(Exception.class));

        // Verify that HEAD and GET requests were made
        verify(mockHttpClient, times(2)).send(any(HttpRequest.class), any());
    }

    @Test
    @DisplayName("should not support ranges if Accept-Ranges header is missing")
    void performDownload_noAcceptRanges_doesNotSupportRanges() throws Exception {
        String fileContent = "Content.";

        // Mock HEAD response without Accept-Ranges
        when(mockHttpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.discarding())))
                .thenReturn(mockHeadResponse);
        when(mockHeadResponse.statusCode()).thenReturn(200);
        when(mockHeadResponse.headers()).thenReturn(HttpHeaders.of(Map.of(), (a, b) -> true));

        // Mock GET response
        when(mockHttpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofInputStream())))
                .thenReturn(mockGetResponse);
        when(mockGetResponse.statusCode()).thenReturn(200);
        when(mockGetResponse.headers()).thenReturn(HttpHeaders.of(
                Map.of("Content-Length",
                        List.of(String.valueOf(fileContent.length()))), (a, b) -> true));
        when(mockGetResponse.body()).thenReturn(new ByteArrayInputStream(fileContent.getBytes()));

        ByteArrayOutputStream destination = new ByteArrayOutputStream();
        spyDownloader.performDownload(testUri, destination, 0); // Use spyDownloader

        assertEquals(fileContent, destination.toString());
    }

    @Test
    @DisplayName("should throw IOException if HEAD request fails")
    void performDownload_headRequestFails_throwsIOException() throws Exception {
        when(mockHttpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.discarding())))
                .thenReturn(mockHeadResponse);
        when(mockHeadResponse.statusCode()).thenReturn(500); // Simulate server error

        ByteArrayOutputStream destination = new ByteArrayOutputStream();

        IOException thrown =
                assertThrows(IOException.class, () -> spyDownloader.performDownload(testUri, destination, 0));
        assertTrue(thrown.getMessage().contains("HEAD request failed with status code: 500"));

        verify(mockHttpClient)
                .send(argThat(req -> req.method().equals("HEAD")),
                        eq(HttpResponse.BodyHandlers.discarding()));
        verify(mockHttpClient, never()).send(argThat(req -> req.method().equals("GET")), any());
    }

    @Test
    @DisplayName("should throw IOException if GET request fails")
    void performDownload_getRequestFails_throwsIOException() throws Exception {
        // Mock successful HEAD response
        when(mockHttpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.discarding())))
                .thenReturn(mockHeadResponse);
        when(mockHeadResponse.statusCode()).thenReturn(200);
        when(mockHeadResponse.headers()).thenReturn(HttpHeaders.of(Map.of(), (a, b) -> true));

        // Mock GET response to return 500
        when(mockHttpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofInputStream())))
                .thenReturn(mockGetResponse);
        when(mockGetResponse.statusCode()).thenReturn(500); // Simulate server error

        ByteArrayOutputStream destination = new ByteArrayOutputStream();

        // Expect an IOException due to the 500 status code
        IOException thrown = assertThrows(
                IOException.class, () -> spyDownloader.performDownload(testUri, destination, 0));
        assertTrue(thrown.getMessage().contains("GET request failed with status code: 500"));

        // Verify that HEAD and GET requests were made
        verify(mockHttpClient).send(
                argThat(req -> req.method().equals("HEAD")), eq(HttpResponse.BodyHandlers.discarding()));
        verify(mockHttpClient).send(
                argThat(req -> req.method().equals("GET")), eq(HttpResponse.BodyHandlers.ofInputStream()));
    }

    @Test
    @DisplayName("should handle InterruptedException gracefully and rethrow as IOException")
    void performDownload_interruptedException_rethrownAsIOException() throws Exception {
        // Mock HEAD request to throw InterruptedException
        when(mockHttpClient.send(any(HttpRequest.class), any()))
                .thenThrow(new InterruptedException("Simulated interruption during HTTP send"));

        ByteArrayOutputStream destination = new ByteArrayOutputStream();

        IOException thrown = assertThrows(
                IOException.class, () -> spyDownloader.performDownload(testUri, destination, 0));
        assertTrue(thrown.getMessage().contains("Download interrupted"));
        assertTrue(thrown.getCause() instanceof InterruptedException);

        verify(mockObserver, never()).onComplete();
        verify(mockObserver, never()).onProgressUpdate(any(Long.class), any(Long.class));
    }
}