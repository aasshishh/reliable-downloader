namespace Accurx.ReliableDownloader.Tests;

[FixtureLifeCycle(LifeCycle.InstancePerTestCase)]
public class FileDownloadTests
{
    private readonly FileDownloader _sut;

    private HttpResponseMessage? _downloadResponse;

    public FileDownloadTests()
    {
        _sut = new FileDownloader(
            new HttpClient(
                new MockHttpMessageHandler((_, _) => Task.FromResult(_downloadResponse!))
            )
        );
    }

    private static async Task<string> GetDownloadedContent(Stream destination)
    {
        destination.Position = 0;
        using var reader = new StreamReader(destination);
        return await reader.ReadToEndAsync();
    }

    private void SetupValidDownload(string content = "Default Content", byte[]? hash = null)
    {
        _downloadResponse = new HttpResponseMessage { Content = new StringContent(content) };
        _downloadResponse.Content.Headers.ContentMD5 = hash;
    }

    [Test]
    public async Task It_downloads_valid_content()
    {
        // Arrange
        const string expectedContent = "Some Content";
        SetupValidDownload(expectedContent);

        // Act
        var destination = new MemoryStream();
        await _sut.DownloadAsync(new Uri("https://example.com/example.msi"), destination);

        // Assert
        var content = await GetDownloadedContent(destination);

        Assert.That(content, Is.EqualTo(expectedContent));
    }

    [Test]
    public async Task It_returns_the_integrity_hash_when_present()
    {
        // Arrange
        byte[] expectedHash = [12, 34, 56, 78, 90];
        SetupValidDownload(hash: expectedHash);

        // Act
        var destination = new MemoryStream();

        var integrityHash = await _sut.DownloadAsync(
            new Uri("https://example.com/example.msi"),
            destination
        );

        // Assert
        Assert.That(integrityHash, Is.EquivalentTo(expectedHash));
    }

    [Test]
    public async Task It_retries_until_the_download_succeeds()
    {
        Assert.Inconclusive("TODO");
    }

    // TODO: Names to be changed to be more descriptive.

    [Test]
    public async Task It_happy_path_download_partial()
    {
        Assert.Inconclusive("TODO");
    }

    [Test]
    public async Task It_sad_path_internet_disconnect_download_partial()
    {
        Assert.Inconclusive("TODO");
    }

    [Test]
    public async Task It_sad_path_internet_disconnect_download_partial_picks_up_where_left_off()
    {
        Assert.Inconclusive("TODO");
    }
}
