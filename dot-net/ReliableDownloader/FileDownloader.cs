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
        var headers = await _webSystemCalls.GetHeadersAsync(contentFileUrl, cancellationToken);
        headers.EnsureSuccessStatusCode();
        var supportsPartial = headers.Headers.AcceptRanges.Contains("bytes");
        var tmpFilePath = $"{headers.Content.Headers.LastModified:yyyyMMddTHHmmss}.download";
        HttpResponseMessage response;
        if (supportsPartial == false)
        {
            response = await _webSystemCalls.DownloadContentAsync(contentFileUrl, cancellationToken);
            await using var responseStream = await response.Content.ReadAsStreamAsync(cancellationToken);
            var destination = File.Open(tmpFilePath, FileMode.OpenOrCreate, FileAccess.ReadWrite);
            await response.Content.CopyToAsync(destination, null, cancellationToken).ConfigureAwait(continueOnCapturedContext: false);

            return true;
        }

        throw new NotImplementedException();
    }
}