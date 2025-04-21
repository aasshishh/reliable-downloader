using Accurx.ReliableDownloader;
using Accurx.ReliableDownloader.Host;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Hosting;

// TODO: Add resilience strategies...
// 1. How would you handle errors during the download process?
// 2. What strategies would you implement to ensure the download is reliable?
// 4. How would you implement a retry mechanism for failed downloads?

var builder = Host.CreateApplicationBuilder(args);
builder
    .Services.Configure<FileDownloadServiceSettings>(
        builder.Configuration.GetSection(FileDownloadServiceSettings.SectionName)
    )
    .AddHttpClient<FileDownloader>()
    .Services.AddHostedService<FileDownloadService>();

using var host = builder.Build();
await host.RunAsync();

// TODO: Additional questions:
// 7. How would you implement logging and monitoring for the download process?
// 8. How would you structure the code to allow for easy testing and maintainability?
// 9. How would you handle different file types and their specific download requirements?
// 10. How would you show download progress and status?
// 11. How would you handle multiple concurrent downloads?
// 12. How would you implement a cancellation mechanism for the download process?
// 13. How would you ensure the application is scalable and can handle increased load?
// 14. How would you implement a configuration system to allow for easy changes to download settings?
// 15. How would you handle different environments (development, staging, production) and their specific configurations?
