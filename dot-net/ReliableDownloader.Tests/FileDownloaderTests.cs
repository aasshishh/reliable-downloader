using NUnit.Framework;

namespace Accurx.ReliableDownloader.Tests;

[FixtureLifeCycle(LifeCycle.InstancePerTestCase)]
public class FileDownloaderTests
{
    private readonly FileDownloader _sut = new(new HttpClient(new HttpClientHandler()));

    [Test]
    public async Task Test1()
    {
        await _sut.DownloadAsync(new Uri("https://example.com/example.msi"), new MemoryStream());

        Assert.Inconclusive("TODO");
    }
}