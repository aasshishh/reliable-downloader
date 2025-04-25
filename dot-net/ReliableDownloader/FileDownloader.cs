﻿using Microsoft.Extensions.Logging;

namespace Accurx.ReliableDownloader;

public sealed class FileDownloader(ILogger<FileDownloader> logger, HttpClient httpClient)
{
    /// <summary>
    /// Downloads a file from the specified URI to the provided stream.
    /// </summary>
    /// <returns>Returns MD5 integrity hash if present</returns>
    public async Task<byte[]?> DownloadAsync(
        Uri source,
        Stream destination,
        CancellationToken cancellationToken = default
    )
    {
        var headRequest = new HttpRequestMessage(HttpMethod.Head, source);
        var headResponse = await httpClient.SendAsync(headRequest, cancellationToken);
        headResponse.EnsureSuccessStatusCode();

        var md5 = headResponse.Content.Headers.ContentMD5;

        using var getResponse = await httpClient.GetAsync(
            source,
            HttpCompletionOption.ResponseHeadersRead,
            cancellationToken
        );
        getResponse.EnsureSuccessStatusCode();

        await using var contentStream = await getResponse.Content.ReadAsStreamAsync(
            cancellationToken
        );

        await contentStream.CopyToAsync(destination, cancellationToken);

        return md5;
    }
}
