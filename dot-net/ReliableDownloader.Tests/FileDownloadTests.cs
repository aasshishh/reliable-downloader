using Accurx.ReliableDownloader.Tests.Helpers;

namespace Accurx.ReliableDownloader.Tests;

[FixtureLifeCycle(LifeCycle.InstancePerTestCase)]
public sealed class FileDownloadTests
{
    private readonly FakeHttpMessageHandler _fakeHandler;
    private readonly FileDownloader _sut;

    public FileDownloadTests()
    {
        _fakeHandler = new FakeHttpMessageHandler();
        _sut = new FileDownloader(new HttpClient(_fakeHandler));
    }

    [Test]
    public async Task It_downloads_valid_content()
    {
        // Arrange
        _fakeHandler.SetupHead();
        var expectedContent = _fakeHandler.SetupValidDownload();

        // Act
        var destination = new MemoryStream();
        await _sut.DownloadAsync(new Uri("https://example.com/example.msi"), destination);

        // Assert
        var content = await destination.GetContentAsync();

        Assert.That(content, Is.EqualTo(expectedContent));
    }

    [Test]
    public async Task It_returns_the_integrity_hash_when_present()
    {
        // Arrange
        byte[] expectedHash = [12, 34, 56, 78, 90];
        _fakeHandler.SetupHead(expectedHash);
        _fakeHandler.SetupValidDownload();

        // Act
        var destination = new MemoryStream();

        var integrityHash = await _sut.DownloadAsync(
            new Uri("https://example.com/example.msi"),
            destination
        );

        // Assert
        Assert.That(integrityHash, Is.EquivalentTo(expectedHash));
    }
}
