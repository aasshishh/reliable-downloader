using ReliableDownloader;

internal sealed class SystemCalls : ISystemCalls
{
    public Stream FileOpen(string localFilePath, FileMode openOrCreate, FileAccess readWrite)
    {
        return File.Open(localFilePath, openOrCreate, readWrite);
    }
}