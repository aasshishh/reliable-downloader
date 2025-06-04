package com.accurx.reliabledownloader.runner;

import com.accurx.reliabledownloader.core.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;

public class Program {
    private static final Logger logger = LoggerFactory.getLogger(Program.class);

    private static final FileDownloadSettings DEFAULT_SETTINGS = new FileDownloadSettings(
            URI.create("https://installer.accurx.com/chain/4.22.50587.0/accuRx.Installer.Local.msi"),
            // URI.create("https://download.oracle.com/java/24/latest/jdk-24_windows-x64_bin.msi"),
            Path.of("./downloads/myfirstdownload.msi"),
            true // Reliable Downloader is enabled by default.
    );

    private static DownloaderConfig createDownloaderConfig(FileDownloadSettings settings) {
        // Create config
        DownloaderConfig.Builder configBuilder = new DownloaderConfig.Builder();

        if (settings.reliableDownloader()) {
            // For poor network conditions
            configBuilder
                    .maxRetries(5)
                    .retryDelay(Duration.ofSeconds(3))
                    .chunkSize(65536) // 64 KB
                    .bufferSize(65536) // 64 KB
                    .connectTimeout(Duration.ofMinutes(1))
                    .readTimeout(Duration.ofMinutes(5));
        } else {
            // For good network conditions
            configBuilder
                    .maxRetries(3)
                    .retryDelay(Duration.ofSeconds(1))
                    .chunkSize(524288) // 512 KB
                    .bufferSize(524288) // 512 KB
                    .connectTimeout(Duration.ofSeconds(1))
                    .readTimeout(Duration.ofSeconds(5));
        }

        return configBuilder.build();
    }

    public static void main(String[] args) {
        try {
            CommandLineSettingsParser parser = new CommandLineSettingsParser(DEFAULT_SETTINGS);
            FileDownloadSettings settings = parser.parse(args);

            logger.info("Starting download from: {}", settings.sourceUrl());
            logger.info("Destination: {}", settings.destinationFilePath());

            DownloaderConfig config = createDownloaderConfig(settings);
            DownloaderFactory factory = new DownloaderFactory();
            // Create downloader based on config & settings
            FileDownloader downloader = factory.createDownloader(config, settings);

            FileDownloadCommand command = new FileDownloadCommand(
                   downloader,
                    settings
            );

            command.run();

            logger.info("Download completed successfully!");
        } catch (Exception e) {
            logger.error("Download failed: {}", e.getMessage(), e);
            System.exit(1);
        }
    }
}