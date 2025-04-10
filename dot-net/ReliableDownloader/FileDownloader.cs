namespace ReliableDownloader;

internal sealed class FileDownloader : IFileDownloader
{
    private readonly IWebSystemCalls _webSystemCalls;
    private readonly ISystemCalls _systemCalls;

    public FileDownloader(IWebSystemCalls webSystemCalls, ISystemCalls systemCalls)
    {
        _webSystemCalls = webSystemCalls;
        _systemCalls = systemCalls;
    }

    public async Task<bool> TryDownloadFile(
        string contentFileUrl,
        string localFilePath,
        Action<FileProgress> onProgressChanged,
        CancellationToken cancellationToken)
    {
        var response = await _webSystemCalls.DownloadContentAsync(contentFileUrl, cancellationToken);
        
        await using var responseStream = await response.Content.ReadAsStreamAsync(cancellationToken);
        
        var destination = _systemCalls.FileOpen(localFilePath, FileMode.OpenOrCreate, FileAccess.ReadWrite);
        
        await response.Content.CopyToAsync(destination, null, cancellationToken);

        return true;
    }
}