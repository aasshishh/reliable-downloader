using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;

namespace ReliableDownloader;

internal static class ServiceCollectionExtensions
{
    public static IServiceCollection AddFileDownloadService(
        this IServiceCollection services,
        IConfiguration configuration)
    {
        services
            .Configure<FileDownloadServiceSettings>(configuration.GetSection(FileDownloadServiceSettings.SectionName))
            .AddHttpClient<IWebSystemCalls, WebSystemCalls>().Services
            .AddSingleton<ISystemCalls, SystemCalls>()
            .AddSingleton<IFileDownloader, FileDownloader>()
            .AddHostedService<FileDownloadService>();
        return services;
    }
}