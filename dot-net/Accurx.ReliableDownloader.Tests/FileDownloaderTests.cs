namespace Accurx.ReliableDownloader.Tests;

[FixtureLifeCycle(LifeCycle.InstancePerTestCase)]
public class Tests
{
    private readonly FileDownloader _sut = new(new HttpClient(new HttpClientHandler()));

    // TODO: Names to be changed to be more descriptive.
    [Test]
    public async Task It_Happy_Path_Download_In_One_Go()
    {
        await _sut.DownloadAsync(new Uri("https://example.com/example.msi"), new MemoryStream());

        Assert.Inconclusive("TODO");
    }

    [Test]
    public async Task It_Sad_Path_Internet_Disconnect_Download_In_One_Go_DeletesFile()
    {
        Assert.Inconclusive("TODO");
    }

    [Test]
    public async Task It_Happy_Path_Download_Partial()
    {

        Assert.Inconclusive("TODO");
    }

    [Test]
    public async Task It_Sad_Path_Internet_Disconnect_Download_Partial()
    {

        Assert.Inconclusive("TODO");
    }

    [Test]
    public async Task It_Sad_Path_Internet_Disconnect_Download_Partial_Picks_Up_Where_Off()
    {

        Assert.Inconclusive("TODO");
    }
}