namespace Accurx.ReliableDownloader.Tests.Helpers;

internal sealed class FakeHttpMessageHandler : HttpMessageHandler
{
    private readonly Queue<HttpResponseMessage> _downloadResponses = new();

    protected override Task<HttpResponseMessage> SendAsync(
        HttpRequestMessage request,
        CancellationToken cancellationToken
    )
    {
        cancellationToken.ThrowIfCancellationRequested();

        return Task.FromResult(_downloadResponses.Dequeue());
    }

    public void Enqueue(HttpResponseMessage response) => _downloadResponses.Enqueue(response);
}
