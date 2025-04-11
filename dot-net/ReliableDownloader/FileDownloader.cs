namespace ReliableDownloader;

internal sealed class FileDownloader(IWebSystemCalls webSystemCalls, ISystemCalls systemCalls) : IFileDownloader
{
    public async Task<bool> TryDownloadFile(
        string contentFileUrl,
        string localFilePath,
        CancellationToken cancellationToken = default)
    {
        var response = await webSystemCalls.DownloadContentAsync(contentFileUrl, cancellationToken);
        
        await using var responseStream = await response.Content.ReadAsStreamAsync(cancellationToken);
        
        var destination = systemCalls.FileOpen(localFilePath, FileMode.OpenOrCreate, FileAccess.ReadWrite);
        
        await response.Content.CopyToAsync(destination, null, cancellationToken);

        return true;
    }
}