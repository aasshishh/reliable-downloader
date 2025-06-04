package com.accurx.reliabledownloader.core;

import java.time.Duration;

public class DownloaderConfig {
    // For handling UNRELIABLE AND SLOW Network conditions
    private static final int DEFAULT_CHUNK_SIZE = 8 * 1024; // 5KB
    private static final int DEFAULT_MAX_RETRIES = 5;
    // Give the network a brief moment to recover. It increases exponentially with each retry.
    private static final Duration DEFAULT_RETRY_DELAY = Duration.ofSeconds(3);
    private static final int DEFAULT_BUFFER_SIZE = 8192; // 8KB
    private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofMinutes(1);
    private static final Duration DEFAULT_READ_TIMEOUT = Duration.ofMinutes(5);

    private final int chunkSize;
    private final int maxRetries;
    private final Duration retryDelay;
    private final int bufferSize;
    private final Duration connectTimeout;
    private final Duration readTimeout;
    private final boolean verifyHash;
    private final boolean resumeSupport;
    private final boolean isProgressTrackingEnabled;

    private DownloaderConfig(Builder builder) {
        this.chunkSize = builder.chunkSize;
        this.maxRetries = builder.maxRetries;
        this.retryDelay = builder.retryDelay;
        this.bufferSize = builder.bufferSize;
        this.connectTimeout = builder.connectTimeout;
        this.readTimeout = builder.readTimeout;
        this.verifyHash = builder.verifyHash;
        this.resumeSupport = builder.resumeSupport;
        this.isProgressTrackingEnabled = builder.isProgressTrackingEnabled;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static DownloaderConfig getDefault() {
        return builder().build();
    }

    // Getters
    public int getChunkSize() {
        return chunkSize;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public Duration getRetryDelay() {
        return retryDelay;
    }

    public int getBufferSize() {
        return bufferSize;
    }

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public Duration getReadTimeout() {
        return readTimeout;
    }

    public boolean isVerifyHash() {
        return verifyHash;
    }

    public boolean isResumeSupport() {
        return resumeSupport;
    }

    public boolean isProgressTrackingEnabled() {
        return isProgressTrackingEnabled;
    }

    public static class Builder {
        private int chunkSize = DEFAULT_CHUNK_SIZE;
        private int maxRetries = DEFAULT_MAX_RETRIES;
        private Duration retryDelay = DEFAULT_RETRY_DELAY;
        private int bufferSize = DEFAULT_BUFFER_SIZE;
        private Duration connectTimeout = DEFAULT_CONNECT_TIMEOUT;
        private Duration readTimeout = DEFAULT_READ_TIMEOUT;
        private boolean verifyHash = true;
        private boolean resumeSupport = true;
        private boolean isProgressTrackingEnabled = true;

        public Builder chunkSize(int chunkSize) {
            this.chunkSize = chunkSize;
            return this;
        }

        public Builder maxRetries(int maxRetries) {
            this.maxRetries = maxRetries;
            return this;
        }

        public Builder retryDelay(Duration retryDelay) {
            this.retryDelay = retryDelay;
            return this;
        }

        public Builder bufferSize(int bufferSize) {
            this.bufferSize = bufferSize;
            return this;
        }

        public Builder connectTimeout(Duration connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }

        public Builder readTimeout(Duration readTimeout) {
            this.readTimeout = readTimeout;
            return this;
        }

        public Builder verifyHash(boolean verifyHash) {
            this.verifyHash = verifyHash;
            return this;
        }

        public Builder progressTrackingEnabled(boolean progressTrackingEnabled) {
            this.isProgressTrackingEnabled = progressTrackingEnabled;
            return this;
        }

        public Builder resumeSupport(boolean resumeSupport) {
            this.resumeSupport = resumeSupport;
            return this;
        }

        public DownloaderConfig build() {
            validate();
            return new DownloaderConfig(this);
        }

        private void validate() {
            if (chunkSize <= 0) {
                throw new IllegalArgumentException("Chunk size must be positive");
            }
            if (maxRetries < 0) {
                throw new IllegalArgumentException("Max retries cannot be negative");
            }
            if (retryDelay.isNegative() || retryDelay.isZero()) {
                throw new IllegalArgumentException("Retry delay must be positive");
            }
            if (bufferSize <= 0) {
                throw new IllegalArgumentException("Buffer size must be positive");
            }
            if (connectTimeout.isNegative() || connectTimeout.isZero()) {
                throw new IllegalArgumentException("Connect timeout must be positive");
            }
            if (readTimeout.isNegative() || readTimeout.isZero()) {
                throw new IllegalArgumentException("Read timeout must be positive");
            }
        }
    }

    @Override
    public String toString() {
        return "DownloaderConfig{" +
                "chunkSize=" + chunkSize +
                ", maxRetries=" + maxRetries +
                ", retryDelay=" + retryDelay +
                ", bufferSize=" + bufferSize +
                ", connectTimeout=" + connectTimeout +
                ", readTimeout=" + readTimeout +
                ", verifyHash=" + verifyHash +
                ", resumeSupport=" + resumeSupport +
                ", isProgressTrackingEnabled=" + isProgressTrackingEnabled +
                '}';
    }
}