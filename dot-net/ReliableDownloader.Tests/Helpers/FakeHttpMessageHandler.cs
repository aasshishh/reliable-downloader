namespace Accurx.ReliableDownloader.Tests.Helpers;

/// <summary>
/// Feel free to swap this fake out with a different approach.
/// </summary>
internal sealed class FakeHttpMessageHandler : HttpMessageHandler
{
    private readonly Queue<Func<HttpRequestMessage, HttpResponseMessage>> _setups = new();

    protected override Task<HttpResponseMessage> SendAsync(
        HttpRequestMessage request,
        CancellationToken cancellationToken
    )
    {
        cancellationToken.ThrowIfCancellationRequested();

        if (_setups.TryDequeue(out var setup))
        {
            var response = setup(request);
            response.RequestMessage = request;
            return Task.FromResult(response);
        }

        throw new AssertionException($"Unexpected '{request.Method} {request.RequestUri}' request");
    }

    public void Enqueue(Func<HttpRequestMessage, HttpResponseMessage> response) =>
        _setups.Enqueue(response);
}
