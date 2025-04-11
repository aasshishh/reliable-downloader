namespace Accurx.ReliableDownloader;

internal interface IFileDownloader
{
    /// <summary>Attempts to download a file and write it to the file system.</summary>
    /// <param name="contentFileUrl">The URL of the file to download.</param>
    /// <param name="localFilePath">The file path to persist the downloaded file to.</param>
    /// <param name="cancellationToken">A cancellation token to cancel the download.</param>
    /// <returns>True if the download completes and writes to the file system successfully, otherwise false.</returns>
    Task<bool> TryDownloadFile(
        string contentFileUrl,
        string localFilePath,
        CancellationToken cancellationToken = default);
}

/// <inheritdoc />
internal sealed class FileDownloader(HttpClient httpClient, ISystemCalls systemCalls) : IFileDownloader
{
    public async Task<bool> TryDownloadFile(
        string contentFileUrl,
        string localFilePath,
        CancellationToken cancellationToken = default)
    {
        using var httpRequestMessage = new HttpRequestMessage(HttpMethod.Get, contentFileUrl);
        var response = await httpClient.SendAsync(httpRequestMessage, cancellationToken);

        await using var responseStream = await response.Content.ReadAsStreamAsync(cancellationToken);

        var destination = systemCalls.FileOpen(localFilePath, FileMode.OpenOrCreate, FileAccess.ReadWrite);

        await response.Content.CopyToAsync(destination, null, cancellationToken);

        return true;
    }
}