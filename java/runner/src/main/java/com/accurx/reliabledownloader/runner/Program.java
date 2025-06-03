package com.accurx.reliabledownloader.runner;

import com.accurx.reliabledownloader.core.FileDownloadCommand;
import com.accurx.reliabledownloader.core.FileDownloadSettings;
import com.accurx.reliabledownloader.impl.FileDownloaderImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.nio.file.Path;

public class Program {
    private static final Logger logger = LoggerFactory.getLogger(Program.class);

    private static final FileDownloadSettings DEFAULT_SETTINGS = new FileDownloadSettings(
            URI.create("https://installer.accurx.com/chain/4.22.50587.0/accuRx.Installer.Local.msi"),
            Path.of("myfirstdownload.msi")
    );

    public static void main(String[] args) {
        try {
            FileDownloadSettings settings = getDownloadSettings(args);

            logger.info("Starting download from: {}", settings.sourceUrl());
            logger.info("Destination: {}", settings.destinationFilePath());

            FileDownloadCommand downloadCommand = new FileDownloadCommand(
                    new FileDownloaderImpl(HttpClient.newBuilder()),
                    settings
            );

            downloadCommand.run();

            logger.info("Download completed successfully!");
        } catch (Exception e) {
            logger.error("Download failed: {}", e.getMessage(), e);
            System.exit(1);
        }
    }

    private static FileDownloadSettings getDownloadSettings(String[] args) {
        if (args.length == 0) {
            logger.info("Using default download settings");
            return DEFAULT_SETTINGS;
        }

        if (args.length != 2) {
            logger.warn("Invalid number of arguments. Expected: <url> <destination>");
            logger.info("Using default download settings");
            return DEFAULT_SETTINGS;
        }

        try {
            URI url = URI.create(args[0]);
            Path destination = Path.of(args[1]);
            return new FileDownloadSettings(url, destination);
        } catch (Exception e) {
            logger.warn("Invalid arguments: {}. Using default settings", e.getMessage());
            return DEFAULT_SETTINGS;
        }
    }
}