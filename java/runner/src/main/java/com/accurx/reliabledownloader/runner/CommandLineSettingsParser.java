package com.accurx.reliabledownloader.runner;

import com.accurx.reliabledownloader.core.FileDownloadSettings;
import com.accurx.reliabledownloader.core.FileDownloadSettingsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

// Assuming this is within a class like SettingsParser or similar
public class CommandLineSettingsParser { // Renamed to illustrate example, use your actual class name
    private final Logger logger = LoggerFactory.getLogger(CommandLineSettingsParser.class); // Assuming SLF4J logger
    private final FileDownloadSettings defaultSettings;

    public CommandLineSettingsParser(FileDownloadSettings defaultSettings) {
        this.defaultSettings = defaultSettings;
    }

    public FileDownloadSettings parse(String[] args) {
        if (args.length == 0) {
            logger.info("No arguments provided. Using default download settings.");
            return defaultSettings;
        }

        Map<String, String> parsedArgs = new HashMap<>();
        for (String arg : args) {
            if (arg.startsWith("--")) { // Check for --key=value format
                int eqIndex = arg.indexOf('=');
                if (eqIndex > 0) {
                    String key = arg.substring(2, eqIndex).trim();
                    String value = arg.substring(eqIndex + 1).trim();
                    parsedArgs.put(key, value);
                } else {
                    logger.warn("Invalid argument format: '{}'. Expected --key=value.", arg);
                }
            } else {
                if (!parsedArgs.containsKey("url")) {
                    parsedArgs.put("url", arg);
                } else if (!parsedArgs.containsKey("destination")) {
                    parsedArgs.put("destination", arg);
                } else {
                    logger.warn("Unexpected positional argument: '{}'. Ignoring.", arg);
                }
            }
        }

        // Initialize with default settings or an empty builder
        FileDownloadSettingsBuilder builder = new FileDownloadSettingsBuilder();

        // Apply parsed arguments
        try {
            if (parsedArgs.containsKey("url")) {
                builder.withSource(URI.create(parsedArgs.get("url")));
            } else {
                logger.warn("URL argument '--url=<url>' is missing. Using default URL.");
                builder.withSource(defaultSettings.sourceUrl()); // Use default if not provided
            }

            if (parsedArgs.containsKey("destination")) {
                builder.withDestination(Path.of(parsedArgs.get("destination")));
            } else {
                logger.warn("Destination argument '--destination=<path>' is missing. Using default destination.");
                builder.withDestination(defaultSettings.destinationFilePath()); // Use default if not provided
            }

            if (parsedArgs.containsKey("network_conditions")) {
                String networkConditionArg = parsedArgs.get("network_conditions").toLowerCase();
                boolean isReliableDownloaderEnabled = true; // Default to poor

                if ("poor".equals(networkConditionArg)) {
                    logger.info("Configuring for poor network conditions.");
                } else if ("good".equals(networkConditionArg)) {
                    isReliableDownloaderEnabled = false;
                    logger.info("Configuring for good network conditions.");
                } else {
                    logger.warn("Invalid network_conditions argument: '{}'. Expected 'good' or 'poor'. Using default network settings (good).", networkConditionArg);
                }
                builder.withReliableDownloader(isReliableDownloaderEnabled);
            } else {
                logger.info("Network conditions not specified. Using default setting.");
                builder.withReliableDownloader(defaultSettings.reliableDownloader()); // Use default if not provided
            }

            return builder.build();

        } catch (Exception e) {
            logger.error("Error parsing arguments: {}", e.getMessage(), e);
            return defaultSettings;
        }
    }
}