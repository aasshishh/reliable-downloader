using System.Text;

namespace Accurx.ReliableDownloader.Tests.Helpers;

internal static class FakeHttpMessageHandlerExtensions
{
    private const string Bytes = "bytes";

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
            response.Headers.AcceptRanges.Add(Bytes);
        }

        fakeHandler.Enqueue(_ => response);
    }

    public static string SetupValidDownload(
        this FakeHttpMessageHandler fakeHandler,
        string content = "Default Content"
    )
    {
        fakeHandler.Enqueue(request =>
        {
            switch (request)
            {
                case { Headers.Range: null }:
                    return new HttpResponseMessage { Content = new StringContent(content) };
                case { Headers.Range.Unit: not Bytes }:
                    throw new NotSupportedException(
                        $"Range header unit {request.Headers.Range.Unit} is not supported"
                    );
                case { Headers.Range: { Unit: Bytes, Ranges.Count: > 0 } }:
                {
                    var bytes = Encoding.UTF8.GetBytes(content);

                    var ranges = request.Headers.Range.Ranges;

                    var chunks = ranges
                        .Select(range => bytes[(int)range.From!..(int)(range.To! - range.From!)])
                        .SelectMany(chunk => chunk)
                        .ToArray();

                    return new HttpResponseMessage { Content = new ByteArrayContent(chunks) };
                }
                default:
                    throw new NotImplementedException("Unexpected request configuration");
            }
        });

        return content;
    }
}
