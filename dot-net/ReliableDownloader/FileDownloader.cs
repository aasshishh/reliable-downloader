namespace Accurx.ReliableDownloader;

public sealed class FileDownloader(HttpClient httpClient)
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

        await Download();

        return contentMd5;

        async Task<(byte[]? ContentMD5, bool AcceptsByteRanges)> ParseHeaders()
        {
            var headRequest = new HttpRequestMessage(HttpMethod.Head, source);
            var headResponse = await httpClient.SendAsync(headRequest, cancellationToken);
            headResponse.EnsureSuccessStatusCode();

            return (
                headResponse.Content.Headers.ContentMD5,
                headResponse.Headers.AcceptRanges.Contains("bytes")
            );
        }

        async Task Download()
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
