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
        var didDownloadSuccessfully = await fileDownloader.TryDownloadFile(
            downloadSettings.Value.SourceUrl,
            Path.Combine(Directory.GetCurrentDirectory(), downloadSettings.Value.DestinationFilePath),
            cancellationToken);

        logger.LogInformation("File download ended! Success: {DownloadSuccessful}", didDownloadSuccessfully);
    }

    public Task StopAsync(CancellationToken cancellationToken)
    {
        logger.LogInformation("FileDownloadService is stopping.");
        return Task.CompletedTask;
    }
}