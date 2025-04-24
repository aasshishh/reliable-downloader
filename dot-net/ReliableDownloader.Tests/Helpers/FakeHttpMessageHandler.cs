namespace Accurx.ReliableDownloader.Tests.Helpers;

/// <summary>
/// A fake implementation of <see cref="HttpMessageHandler"/> for testing purposes.
/// Allows enqueuing predefined responses to simulate HTTP requests.
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

        throw new AssertionException(
            $"Unexpected '{request.Method} {request.RequestUri}' request, have you missed a setup?"
        );
    }

    /// <summary>
    /// Enqueues a predefined response for a simulated HTTP request.
    /// </summary>
    /// <param name="response">A function that generates an HTTP response based on the request.</param>
    public void Enqueue(Func<HttpRequestMessage, HttpResponseMessage> response) =>
        _setups.Enqueue(response);
}
