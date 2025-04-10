namespace ReliableDownloader;

internal sealed class FileDownloader : IFileDownloader
{
    private readonly WebSystemCalls _webSystemCalls;

    public FileDownloader(WebSystemCalls webSystemCalls)
    {
        _webSystemCalls = webSystemCalls;
    }

    public async Task<bool> TryDownloadFile(
        string contentFileUrl,
        string localFilePath,
        Action<FileProgress> onProgressChanged,
        CancellationToken cancellationToken)
    {
        var response = await _webSystemCalls.DownloadContentAsync(contentFileUrl, cancellationToken);
        
        await using var responseStream = await response.Content.ReadAsStreamAsync(cancellationToken);
        
        var destination = File.Open(localFilePath, FileMode.OpenOrCreate, FileAccess.ReadWrite);
        
        await response.Content.CopyToAsync(destination, null, cancellationToken);

        return true;
    }
}