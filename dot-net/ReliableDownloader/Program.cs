using Accurx.ReliableDownloader;
using Microsoft.Extensions.Hosting;

var builder = Host.CreateApplicationBuilder(args);
builder.Services.AddFileDownloadService(builder.Configuration);

using var host = builder.Build();
await host.RunAsync();