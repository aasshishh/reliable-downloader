using NUnit.Framework;

namespace Accurx.ReliableDownloader.Tests;

[FixtureLifeCycle(LifeCycle.InstancePerTestCase)]
public class FileDownloaderTests
{
    private readonly FileDownloader _sut = new(new HttpClient(new HttpClientHandler()), new SystemCalls());

    [Test]
    public async Task Test1()
    {
        await _sut.TryDownloadFile("https://example.com/example.msi", "example.msi", default);

        Assert.Inconclusive("TODO");
    }
}