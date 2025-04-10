using NUnit.Framework;

namespace ReliableDownloader.Tests;

[FixtureLifeCycle(LifeCycle.InstancePerTestCase)]
public class Tests
{
    private readonly FileDownloader _sut = new(new WebSystemCalls(), new SystemCalls());

    // TODO: Names to be changed to be more descriptive.
    [Test]
    public async Task It_Happy_Path_Download_In_One_Go()
    {
        await _sut.TryDownloadFile("https://example.com/example.msi", "example.msi", _ => { });

        Assert.Inconclusive("TODO");
    }

    [Test]
    public async Task It_Sad_Path_Internet_Disconnect_Download_In_One_Go_DeletesFile()
    {
        await _sut.TryDownloadFile("https://example.com/example.msi", "example.msi", _ => { });

        Assert.Inconclusive("TODO");
    }

    [Test]
    public async Task It_Happy_Path_Download_Partial()
    {
        await _sut.TryDownloadFile("https://example.com/example.msi", "example.msi", _ => { });

        Assert.Inconclusive("TODO");
    }

    [Test]
    public async Task It_Sad_Path_Internet_Disconnect_Download_Partial()
    {
        await _sut.TryDownloadFile("https://example.com/example.msi", "example.msi", _ => { });

        Assert.Inconclusive("TODO");
    }

    [Test]
    public async Task It_Sad_Path_Internet_Disconnect_Download_Partial_Picks_Up_Where_Off()
    {
        await _sut.TryDownloadFile("https://example.com/example.msi", "example.msi", _ => { });

        Assert.Inconclusive("TODO");
    }
}