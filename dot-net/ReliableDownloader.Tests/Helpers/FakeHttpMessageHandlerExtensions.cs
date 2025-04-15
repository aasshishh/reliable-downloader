namespace Accurx.ReliableDownloader.Tests.Helpers;

internal static class FakeHttpMessageHandlerExtensions
{
    public static void SetupHead(this FakeHttpMessageHandler fakeHandler, byte[]? hash = null)
    {
        var response = new HttpResponseMessage();
        response.Content.Headers.ContentMD5 = hash;
        fakeHandler.Enqueue(response);
    }

    public static string SetupValidDownload(
        this FakeHttpMessageHandler fakeHandler,
        string content = "Default Content"
    )
    {
        fakeHandler.Enqueue(new HttpResponseMessage { Content = new StringContent(content) });
        return content;
    }
}
