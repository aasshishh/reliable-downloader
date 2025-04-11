using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;

namespace Accurx.ReliableDownloader;

internal static class ServiceCollectionExtensions
{
    public static IServiceCollection AddFileDownloadService(
        this IServiceCollection services,
        IConfiguration configuration)
    {
        services
            .Configure<FileDownloadServiceSettings>(configuration.GetSection(FileDownloadServiceSettings.SectionName))
            .AddHttpClient<IFileDownloader, FileDownloader>().Services
            .AddHostedService<FileDownloadService>();
        return services;
    }
}