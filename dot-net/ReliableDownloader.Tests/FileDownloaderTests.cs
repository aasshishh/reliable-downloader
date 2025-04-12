using NUnit.Framework;

namespace Accurx.ReliableDownloader.Tests;

[FixtureLifeCycle(LifeCycle.InstancePerTestCase)]
public class FileDownloaderTests
{
    [Test]
    public async Task It_downloads_the_content()
    {
        // Arrange
        const string expectedContent = "ExampleContent";

        var sut = new FileDownloader(
            new HttpClient(
                new MockHttpMessageHandler(
                    (_, _) =>
                        Task.FromResult(
                            new HttpResponseMessage { Content = new StringContent(expectedContent) }
                        )
                )
            )
        );

        // Act
        var destination = new MemoryStream();
        await sut.TryDownloadAsync(new Uri("https://example.com/example.msi"), destination);

        // Assert
        destination.Position = 0;
        using var reader = new StreamReader(destination);
        var content = await reader.ReadToEndAsync();

        Assert.That(content, Is.EqualTo(expectedContent));
    }
}
