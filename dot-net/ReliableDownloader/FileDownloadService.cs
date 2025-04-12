using System.Security.Cryptography;
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
    IFileDownloader fileDownloader
) : IHostedService
{
    public async Task StartAsync(CancellationToken cancellationToken)
    {
        await using var destination = File.Open(
            downloadSettings.Value.DestinationFilePath,
            FileMode.OpenOrCreate,
            FileAccess.ReadWrite
        );

        var contentMd5 = await fileDownloader.TryDownloadAsync(
            new Uri(downloadSettings.Value.SourceUrl),
            destination,
            cancellationToken
        );

        if (contentMd5 is null)
        {
            logger.LogWarning("File download was not validated");
        }
        else
        {
            using var md5 = MD5.Create();
            destination.Position = 0;

            var computedMd5 = await md5.ComputeHashAsync(destination, cancellationToken);

            if (computedMd5.SequenceEqual(contentMd5))
            {
                logger.LogInformation("File download succeeded");
            }
            else
            {
                logger.LogError("File download failed!");
            }
        }
    }

    public Task StopAsync(CancellationToken cancellationToken)
    {
        logger.LogInformation("FileDownloadService is stopping.");
        return Task.CompletedTask;
    }
}
