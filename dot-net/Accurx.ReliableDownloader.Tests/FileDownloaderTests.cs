namespace Accurx.ReliableDownloader.Tests;

[FixtureLifeCycle(LifeCycle.InstancePerTestCase)]
public class Tests
{
    // TODO: Names to be changed to be more descriptive.
    [Test]
    public async Task It_Happy_Path_Download_In_One_Go()
    {
        // Arrange
        const string expectedContent = "ExampleFileContent";
        
        var sut = new FileDownloader(new HttpClient(new MockHttpMessageHandler((_, _) =>
            Task.FromResult(new HttpResponseMessage { Content = new StringContent(expectedContent) }))));
        
        // Act
        var destination = new MemoryStream();
        await sut.DownloadAsync(new Uri("https://example.com/example.msi"), destination);
        
        // Assert
        destination.Position = 0;
        using var reader = new StreamReader(destination);
        var content = await reader.ReadToEndAsync();
        
        Assert.That(content, Is.EqualTo(expectedContent));
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