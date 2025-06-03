package com.accurx.reliabledownloader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.println("Usage: java -jar reliabledownloader.jar <url> <destination> <expected-hash>");
            System.exit(1);
        }

        try {
            String url = args[0];
            Path destination = Path.of(args[1]);
            String expectedHash = args[2];

            logger.info("Starting download from: {}", url);
            logger.info("Destination: {}", destination);

            ReliableDownloader downloader = new ReliableDownloader(url, destination, expectedHash);
            downloader.download();

            logger.info("Download completed successfully!");
        } catch (Exception e) {
            logger.error("Download failed: {}", e.getMessage(), e);
            System.exit(1);
        }
    }
}