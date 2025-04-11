using Microsoft.Extensions.Hosting;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Options;

namespace Accurx.ReliableDownloader;

internal record FileDownloadServiceSettings
{
    public static string SectionName => "FileDownloadService";

    public required string SourceUrl { get; init; }
    public required string DestinationFilePath { get; init; }
}

internal sealed class FileDownloadService(
    ILogger<FileDownloadService> logger,
    IOptions<FileDownloadServiceSettings> downloadSettings,
    IFileDownloader fileDownloader)
    : IHostedService
{
    public async Task StartAsync(CancellationToken cancellationToken)
    {
        await using var destination = File.Open(downloadSettings.Value.DestinationFilePath, FileMode.OpenOrCreate,
            FileAccess.ReadWrite);

        await fileDownloader.DownloadAsync(
            new Uri(downloadSettings.Value.SourceUrl), destination,
            cancellationToken);

        logger.LogInformation("File download ended!");
    }

    public Task StopAsync(CancellationToken cancellationToken)
    {
        logger.LogInformation("FileDownloadService is stopping.");
        return Task.CompletedTask;
    }
}