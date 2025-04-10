namespace ReliableDownloader;

internal interface ISystemCalls
{
    Stream FileOpen(string localFilePath, FileMode openOrCreate, FileAccess readWrite);
}