package com.accurx.reliabledownloader.mocks;

import com.google.common.hash.Hashing;
import com.google.common.io.BaseEncoding;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okio.Buffer;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * A fake Cdn implementation for testing purposes that simulates file downloads
 * It provides two URIs, one that accepts range requests and one that does not.
 */
public class FakeCdn implements BeforeAllCallback, AfterAllCallback {

    private static final String ACCEPT_RANGES_HEADER = "Accept-Ranges";
    private static final String CONTENT_DISPOSITION_HEADER = "Content-Disposition";
    private static final String CONTENT_MD5_HEADER = "Content-MD5";
    private static final String CONTENT_LENGTH = "Content-Length";
    private static final String ACCEPTS_RANGES_BYTES = "bytes";

    private final String fileName;
    private final String content;
    private final String contentHash;
    private final String acceptRangesPath;
    private final String noAcceptRangesPath;
    private final MockWebServer server;

    public FakeCdn(String fileName, String content) {
        this.fileName = fileName;
        this.acceptRangesPath = "/accept-ranges/" + fileName;
        this.noAcceptRangesPath = "/no-accept-ranges/" + fileName;
        this.content = content;
        // Hash the content as bytes to match how Md5.contentMd5() computes the hash from files
        byte[] contentBytes = content.getBytes(StandardCharsets.UTF_8);
        this.contentHash = BaseEncoding.base64().encode(
                Hashing.md5().hashBytes(contentBytes).asBytes()
        );
        this.server = new MockWebServer();
        this.server.setDispatcher(new CndDispatcher());
    }

    public URI getNoRangeUri()
    {
        return server.url(noAcceptRangesPath).uri();
    }

    public URI getAcceptRangesUri()
    {
        return server.url(acceptRangesPath).uri();
    }

    public String getContent() {
        return content;
    }

    public String getContentHash()
    {
        return contentHash;
    }

    public MockWebServer getServer() {
        return server;
    }

    @Override
    public void afterAll(ExtensionContext extensionContext) throws Exception {
        server.shutdown();
    }

    @Override
    public void beforeAll(ExtensionContext extensionContext) throws Exception {
        server.start();
    }

    private class CndDispatcher extends Dispatcher {

        @Override
        public MockResponse dispatch(RecordedRequest request) {
            String path = request.getPath();
            String method = request.getMethod();

            // Handle HEAD requests specifically for non-existent files
            if (path.equals("/nonexistent-path/file.txt") && "HEAD".equals(method)) {
                return new MockResponse().setResponseCode(404); // Explicit 404 for HEAD
            }
            // Handle GET requests for non-existent files
            if (path.equals("/nonexistent-path/file.txt") && "GET".equals(method)) {
                return new MockResponse().setResponseCode(404); // Explicit 404 for GET
            }

            // Handle HEAD requests specifically for non-existent paths
            if (path.equals("/invalid-server-error-path/file.txt") && "HEAD".equals(method)) {
                return new MockResponse().setResponseCode(501); // Explicit 404 for HEAD
            }
            // Handle GET requests for non-existent paths (if you uncomment the test)
            if (path.equals("/invalid-server-error-path/file.txt") && "GET".equals(method)) {
                return new MockResponse().setResponseCode(501); // Explicit 404 for GET
            }

            MockResponse mockResponse = new MockResponse();
            if (noAcceptRangesPath.equals(request.getPath())) {
                // Don't add Accept-Ranges header
            } else if (acceptRangesPath.equals(request.getPath())) {
                mockResponse.addHeader(ACCEPT_RANGES_HEADER, ACCEPTS_RANGES_BYTES);
            } else {
                throw new UnsupportedOperationException("Unsupported request path " + request.getPath());
            }

            if ("HEAD".equals(request.getMethod())) {
                String rangeHeader = request.getHeader("Range");
                if (rangeHeader != null && acceptRangesPath.equals(request.getPath())) {
                    // Handle HEAD request with Range header for resume consistency check
                    Range downloadRange = Range.parseOne(rangeHeader);
                    byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
                    
                    int from = downloadRange.start;
                    // Validate range for HEAD request
                    if (from >= bytes.length || from < 0) {
                        mockResponse.setResponseCode(416); // Range Not Satisfiable
                        return mockResponse;
                    } else {
                        // For HEAD with valid range, return 206 with appropriate Content-Length
                        int to = (downloadRange.end == Integer.MAX_VALUE) ? bytes.length : Math.min(downloadRange.end + 1, bytes.length);
                        mockResponse.setResponseCode(206);
                        mockResponse.addHeader(CONTENT_LENGTH, to - from);
                        mockResponse.addHeader("Content-Range", "bytes " + from + "-" + (to - 1) + "/" + bytes.length);
                    }
                } else if (rangeHeader != null && noAcceptRangesPath.equals(request.getPath())) {
                    // Server that doesn't accept ranges should ignore Range header and return 200 OK
                    mockResponse.addHeader(CONTENT_LENGTH, content.getBytes(StandardCharsets.UTF_8).length);
                } else {
                    // Normal HEAD request without Range header
                    mockResponse.addHeader(CONTENT_LENGTH, content.getBytes(StandardCharsets.UTF_8).length);
                }
            } else if ("GET".equals(request.getMethod())) {
                String rangeHeader = request.getHeader("Range");
                if (rangeHeader == null) {
                    byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
                    mockResponse.setBody(new Buffer().write(bytes));
                    mockResponse.addHeader(CONTENT_LENGTH, bytes.length);
                } else {
                    handlePartialDownload(rangeHeader, mockResponse);
                }
            }

            mockResponse.addHeader(CONTENT_DISPOSITION_HEADER,"attachment; filename=\"" +  fileName + "\"");
            mockResponse.addHeader(CONTENT_MD5_HEADER, contentHash);

            return mockResponse;
        }

        private void handlePartialDownload(String rangeHeader, MockResponse mockResponse) {

            Range downloadRange = Range.parseOne(rangeHeader);

            byte[] bytes = content.getBytes(StandardCharsets.UTF_8);

            int from = downloadRange.start;
            // Handle open-ended ranges (e.g., "bytes=10-") by setting 'to' to the end of the content
            int to = (downloadRange.end == Integer.MAX_VALUE) ? bytes.length : downloadRange.end + 1;

            // Validate range
            if (from >= bytes.length || from < 0) {
                mockResponse.setResponseCode(416); // Range Not Satisfiable
            } else {
                // Clamp 'to' to the actual content length
                to = Math.min(to, bytes.length);

                Buffer buffer = new Buffer().write(Arrays.copyOfRange(bytes, from, to));
                mockResponse.setResponseCode(206);
                mockResponse.setBody(buffer);
                mockResponse.setHeader(CONTENT_LENGTH, to - from);
            }
        }
    }

    private record Range(int start, int end) {

        private static final Pattern rangeWithEndMatcher = Pattern.compile("^bytes=(?<from>\\d+)-(?<to>\\d+)$");
        private static final Pattern rangeWithoutEndMatcher = Pattern.compile("^bytes=(?<from>\\d+)-$");

        public static Range parseOne(String rangeHeader) {
            // Try pattern with both start and end bytes first (e.g., "bytes=10-20")
            Matcher matcherWithEnd = rangeWithEndMatcher.matcher(rangeHeader);
            if (matcherWithEnd.matches()) {
                return new Range(
                    Integer.parseInt(matcherWithEnd.group("from")), 
                    Integer.parseInt(matcherWithEnd.group("to"))
                );
            }
            
            // Try pattern with only start byte (e.g., "bytes=10-")
            Matcher matcherWithoutEnd = rangeWithoutEndMatcher.matcher(rangeHeader);
            if (matcherWithoutEnd.matches()) {
                int from = Integer.parseInt(matcherWithoutEnd.group("from"));
                // For ranges without end, use a large number to indicate "to end of file"
                // This will be handled properly in handlePartialDownload method
                return new Range(from, Integer.MAX_VALUE);
            }
            
            throw new UnsupportedOperationException("Unsupported range header " + rangeHeader);
        }
    }
}

