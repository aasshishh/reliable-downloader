using System.Net.Http.Headers;

namespace ReliableDownloader;

internal sealed class WebSystemCalls(HttpClient client) : IWebSystemCalls
{
    public async Task<HttpResponseMessage> GetHeadersAsync(string url, CancellationToken token)
    {
        using var httpRequestMessage = new HttpRequestMessage(HttpMethod.Head, url);
        return await client.SendAsync(httpRequestMessage, token).ConfigureAwait(continueOnCapturedContext: false);
    }

    public async Task<HttpResponseMessage> DownloadContentAsync(string url, CancellationToken token)
    {
        using var httpRequestMessage = new HttpRequestMessage(HttpMethod.Get, url);
        return await client.SendAsync(httpRequestMessage, token).ConfigureAwait(continueOnCapturedContext: false);
    }

    public async Task<HttpResponseMessage> DownloadPartialContentAsync(string url, long from, long to, CancellationToken token)
    {
        using var httpRequestMessage = new HttpRequestMessage(HttpMethod.Get, url);
        httpRequestMessage.Headers.Range = new RangeHeaderValue(from, to);
        return await client.SendAsync(httpRequestMessage, token).ConfigureAwait(continueOnCapturedContext: false);
    }
}