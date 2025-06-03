package com.accurx.reliabledownloader.runner;

import com.accurx.reliabledownloader.core.*;
import com.accurx.reliabledownloader.impl.ConsoleProgressObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;

public class Program {
    private static final Logger logger = LoggerFactory.getLogger(Program.class);

    private static final FileDownloadSettings DEFAULT_SETTINGS = new FileDownloadSettings(
            URI.create("https://installer.accurx.com/chain/4.22.50587.0/accuRx.Installer.Local.msi"),
            Path.of("myfirstdownload.msi"),
            "ReliableDownloader"
    );

    public static void main(String[] args) {
        try {
            CommandLineSettingsParser parser = new CommandLineSettingsParser(DEFAULT_SETTINGS);
            FileDownloadSettings settings = parser.parse(args);

            logger.info("Starting download from: {}", settings.sourceUrl());
            logger.info("Destination: {}", settings.destinationFilePath());

            // Create config
            DownloaderConfig.Builder configBuilder = new DownloaderConfig.Builder()
                    .maxRetries(3)
                    .connectTimeout(Duration.ofMinutes(5));

            DownloaderFactory factory = new DownloaderFactory();
            // Create downloader based on config & settings
            FileDownloader downloader = factory.createDownloader(configBuilder.build(), settings);

            FileDownloadCommand command = new FileDownloadCommand(
                   downloader,
                    settings
            );
            command.addObserver(new ConsoleProgressObserver());

            command.run();

            logger.info("Download completed successfully!");
        } catch (Exception e) {
            logger.error("Download failed: {}", e.getMessage(), e);
            System.exit(1);
        }
    }
}