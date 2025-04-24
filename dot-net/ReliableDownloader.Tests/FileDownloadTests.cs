using Accurx.ReliableDownloader.Tests.Helpers;
using Microsoft.Extensions.Logging.Abstractions;

namespace Accurx.ReliableDownloader.Tests;

/// <remarks>
/// Feel free to swap out NUnit for any other test framework you may prefer and to use an alternative for mocking the
/// HttpClient/HttpMessageHandler.
///
/// We're looking for unit tests only around your range requests implementation, please test your resilience
/// implementation manually by running the Host project, we're not expecting you to write integration tests.
/// </remarks>>
[FixtureLifeCycle(LifeCycle.InstancePerTestCase)]
public sealed class FileDownloadTests
{
    private readonly FakeHttpMessageHandler _fakeHandler;
    private readonly FileDownloader _sut;

    public FileDownloadTests()
    {
        _fakeHandler = new FakeHttpMessageHandler();
        _sut = new FileDownloader(new NullLogger<FileDownloader>(), new HttpClient(_fakeHandler));
    }

    [Test]
    public async Task It_downloads_content_when_accept_ranges_is_not_supported()
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

    [Test]
    public async Task It_downloads_content_in_chunks_when_accept_ranges_is_supported()
    {
        // Arrange
        _fakeHandler.SetupHead(acceptRanges: "bytes");

        // Act
        var destination = new MemoryStream();
        await _sut.DownloadAsync(new Uri("https://example.com/example.msi"), destination);

        // Assert
        Assert.Inconclusive("TODO");
    }
}
