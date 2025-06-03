package com.accurx.reliabledownloader.runner;

import com.accurx.reliabledownloader.core.FileDownloadSettings;
import com.accurx.reliabledownloader.core.FileDownloadSettingsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.nio.file.Path;

public class CommandLineSettingsParser {
    private static final Logger logger = LoggerFactory.getLogger(CommandLineSettingsParser.class);
    private final FileDownloadSettings defaultSettings;

    public CommandLineSettingsParser(FileDownloadSettings defaultSettings) {
        this.defaultSettings = defaultSettings;
    }

    public FileDownloadSettings parse(String[] args) {
        if (args.length == 0) {
            logger.info("Using default download settings");
            return defaultSettings;
        }

        if (args.length != 2) {
            logger.warn("Invalid number of arguments. Expected: <url> <destination>");
            logger.info("Using default download settings");
            return defaultSettings;
        }

        try {
            URI url = URI.create(args[0]);
            Path destination = Path.of(args[1]);
            return new FileDownloadSettingsBuilder()
                    .withSource(url)
                    .withDestination(destination)
                    .build();
        } catch (Exception e) {
            logger.warn("Invalid arguments: {}. Using default settings", e.getMessage());
            return defaultSettings;
        }
    }
}