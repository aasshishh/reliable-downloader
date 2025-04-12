namespace Accurx.ReliableDownloader;

internal interface IFileDownloader
{
    Task<byte[]?> TryDownloadAsync(
        Uri source,
        Stream destination,
        CancellationToken cancellationToken = default
    );
}

internal sealed class FileDownloader(HttpClient httpClient) : IFileDownloader
{
    public async Task<byte[]?> TryDownloadAsync(
        Uri source,
        Stream destination,
        CancellationToken cancellationToken = default
    )
    {
        var (contentMd5, acceptsByteRanges) = await ParseHeaders();

        // TODO: Add partial content support based on acceptsByteRanges...

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
