using Microsoft.Extensions.Hosting;
using Microsoft.Extensions.Logging;
using Microsoft.Extensions.Options;

namespace ReliableDownloader;

internal record FileDownloadServiceSettings
{
    public static string SectionName => "FileDownloadService";

    public required string SourceUrl { get; init; }
    public required string DestinationFilePath { get; init; }
}

internal sealed class FileDownloadService : IHostedService
{
    private readonly ILogger<FileDownloadService> _logger;
    private readonly IOptions<FileDownloadServiceSettings> _downloadSettings;
    private readonly IFileDownloader _fileDownloader;

    public FileDownloadService(
        ILogger<FileDownloadService> logger,
        IOptions<FileDownloadServiceSettings> downloadSettings,
        IFileDownloader fileDownloader)
    {
        _logger = logger;
        _downloadSettings = downloadSettings;
        _fileDownloader = fileDownloader;
    }

    public async Task StartAsync(CancellationToken cancellationToken)
    {
        var didDownloadSuccessfully = await _fileDownloader.TryDownloadFile(
            _downloadSettings.Value.SourceUrl,
            Path.Combine(Directory.GetCurrentDirectory(), _downloadSettings.Value.DestinationFilePath), cancellationToken);

        _logger.LogInformation("File download ended! Success: {DownloadSuccessful}", didDownloadSuccessfully);
    }

    public Task StopAsync(CancellationToken cancellationToken)
    {
        _logger.LogInformation("FileDownloadService is stopping.");
        return Task.CompletedTask;
    }
}