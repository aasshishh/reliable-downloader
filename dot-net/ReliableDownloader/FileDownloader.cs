using Microsoft.Extensions.Logging;

namespace Accurx.ReliableDownloader;

public sealed class FileDownloader(ILogger<FileDownloader> logger, HttpClient httpClient)
{
    /// <summary>
    /// Downloads a file from the specified URI to the provided stream.
    /// </summary>
    /// <returns>Returns MD5 integrity hash if present</returns>
    public async Task<byte[]?> DownloadAsync(
        Uri source,
        Stream destination,
        CancellationToken cancellationToken = default
    )
    {
        var (contentMd5, acceptsByteRanges) = await ParseHeaders();

        // TODO: Add partial content support based on acceptsByteRanges...
        // 3. How would you handle large files or slow network conditions?

        await DownloadFull();

        return contentMd5;

        async Task<(byte[]? ContentMD5, bool AcceptsByteRanges)> ParseHeaders()
        {
            var headRequest = new HttpRequestMessage(HttpMethod.Head, source);
            var headResponse = await httpClient.SendAsync(headRequest, cancellationToken);
            headResponse.EnsureSuccessStatusCode();

            var md5 = headResponse.Content.Headers.ContentMD5;
            if (md5 is not null)
            {
                logger.LogInformation("MD5 hash detected: {MD5}", md5);
            }

            var byteRanges = headResponse.Headers.AcceptRanges.Contains(
                "bytes",
                StringComparer.OrdinalIgnoreCase
            );

            if (byteRanges)
            {
                logger.LogInformation("Support for byte ranges detected.");
            }

            return (md5, byteRanges);
        }

        async Task DownloadFull()
        {
            using var getResponse = await httpClient.GetAsync(
                source,
                HttpCompletionOption.ResponseHeadersRead,
                cancellationToken
            );
            getResponse.EnsureSuccessStatusCode();

            await using var contentStream = await getResponse.Content.ReadAsStreamAsync(
                cancellationToken
            );

            await contentStream.CopyToAsync(destination, cancellationToken);
        }
    }
}
