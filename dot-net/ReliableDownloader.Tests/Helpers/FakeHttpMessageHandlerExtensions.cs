namespace Accurx.ReliableDownloader.Tests.Helpers;

internal static class FakeHttpMessageHandlerExtensions
{
    public static void SetupHead(
        this FakeHttpMessageHandler fakeHandler,
        byte[]? hash = null,
        string? acceptRanges = null
    )
    {
        var response = new HttpResponseMessage();
        response.Content.Headers.ContentMD5 = hash;

        if (acceptRanges is not null)
        {
            response.Headers.AcceptRanges.Add("bytes");
        }

        fakeHandler.Enqueue(_ => response);
    }

    public static string SetupValidDownload(
        this FakeHttpMessageHandler fakeHandler,
        string content = "Default Content"
    )
    {
        fakeHandler.Enqueue(_ => new HttpResponseMessage { Content = new StringContent(content) });
        return content;
    }
}
