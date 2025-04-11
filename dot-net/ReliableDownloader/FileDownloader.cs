namespace Accurx.ReliableDownloader;

internal interface IFileDownloader
{
    /// <summary>Attempts to download a file and write it to the file system.</summary>
    /// <param name="source"></param>
    /// <param name="destination"></param>
    /// <param name="cancellationToken">A cancellation token to cancel the download.</param>
    /// <returns>True if the download completes and writes to the file system successfully, otherwise false.</returns>
    Task DownloadAsync(Uri source, Stream destination, CancellationToken cancellationToken = default);
}

/// <inheritdoc />
internal sealed class FileDownloader(HttpClient httpClient) : IFileDownloader
{
    public async Task DownloadAsync(
        Uri source,
        Stream destination,
        CancellationToken cancellationToken = default)
    {
        using var request = new HttpRequestMessage(HttpMethod.Get, source);
        var response = await httpClient.SendAsync(request, cancellationToken);

        await using var responseStream = await response.Content.ReadAsStreamAsync(cancellationToken);

        await response.Content.CopyToAsync(destination, null, cancellationToken);
    }
}