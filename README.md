# PROJECT REPORT
## Objective: File Downloader Implementation

This project delivers a versatile file downloader, designed to perform reliably across diverse network environments. Its core capabilities address challenges in unstable network conditions through:

*   **Advanced Download Mechanisms:**
  *   Range request validation and utilization.
  *   Efficient chunked downloading for improved throughput and resilience.
  *   Automatic resume functionality for interrupted transfers.
  *   Configurable retry mechanisms to overcome transient network issues.


*   **Data Integrity & User Experience:**
  *   Post-download hash verification to guarantee file integrity.
  *   Real-time progress tracking.
  *   Robust error handling to manage download failures gracefully.

For optimal performance in stable network environments, a dedicated `HTTPClient` based implementation is also provided.

The difference between `HTTPClientFileDownloader` and `ReliableDownloader` are as follows.
1. `HTTPClientFileDownloader` uses `java.net.http.HttpClient` (a newer, higher level modern API) compared to `java.net.HttpURLConnection` used by `ReliableDownloader`,
2. While `ReliableDownloader` explicitly manages chunking logic with *Accept-Ranges* header, `HTTPClientFileDownloader` handles entire download.
3. `ReliableDownloader` performs MD5 verification

## File Structures
- Provide a high-level overview of the project's directory and file organization.
- You can use a tree-like structure or list key directories and their contents.
- Example:
``` 
    ├── library/
    │ ├── src/
    │ │   ├── main/
    │ │   │   ├── java/
    │ │   │   │   └── com/accurx/reliabledownloader/
    │ │   │   │       ├── core/                 // Interfaces and Abstract classes
    │ │   │   │       ├── impl/                 // Implementation classes
    │ │   │   │       ├── util/                 // Utility classes
    │ │   │   └── resources/            // Configuration (static files can be added here)
    │ │   └── test/
    │ │       └── java/
    │ │           └── com/accurx/reliabledownloader/
    │ │  
    │ ├── build/
    │ └── build.gradle
    └── runner/
      ├── src/
      │   ├── main/
      │   │   └── java/
      │   │       └── com/accurx/reliabledownloader/runner/
      │   │           ├── CommandLineSettingsParser     // Command line parser
      │   │           └── Program                       // Main Class
      │   └── test/
      │       └──  java/
      │           └── com/accurx/reliabledownloader/runner/
      │               └── ...                 // Test classes
      ├── build                      
      └── build.gradle 
```
## Core Functionalities

The project provides a resilient file downloader with the following key features:
*   **File Download Capability:** Secure and efficient file transfer.
*   **Progress Monitoring:** Real-time feedback on download status.
*   **Robust Error Management:** Handles and reports various download issues.
*   **Automated Retries:** Includes intelligent retry logic, potentially with exponential backoff.
*   **Download Resumption:** Ability to continue interrupted transfers.
*   **Network Condition Configuration:** Adaptable settings for different network qualities.

## Code Design
### Adherence to Best Practices

During the development of this project, I rigorously adhered to several key software engineering best practices to ensure the creation of a high-quality, maintainable, and scalable system:

*   **Clean Code Principles:** Emphasized readability, clarity, and maintainability. This involved writing self-documenting code, using meaningful variable and method names, keeping functions small and focused, and adhering to consistent formatting.
*   **Modularity & Single Responsibility Principle (SRP):** A strong focus on modularity, breaking down complex functionalities into independent, reusable modules.
*   **Extensibility:** Ensured the system's extensibility, allowing for easy integration of new features or modifications without significantly impacting existing functionalities. This was achieved through clear interfaces and loosely coupled components.
*   **Robust Error Handling:** Comprehensive error detection and recovery mechanisms were implemented throughout the application.
*   **Effective Logging:** Logging was integrated to provide clear insights into application behavior, aiding in debugging, performance monitoring, and tracing execution flows during development.

### Design Patterns Implemented

The project leverages several established design patterns to achieve a modular, flexible, and extensible architecture:

1.  **Decorator Pattern:** Dynamically adds retry and progress tracking to `FileDownloader` (`RetryingDownloader`, `ProgressTrackingDownloader`), extending capabilities without modifying core code.
2.  **Factory Method Pattern:** `DownloaderFactory` abstracts `FileDownloader` instantiation, decoupling client code from concrete implementations and their decorators.
3.  **Command Pattern:** `FileDownloadCommand` encapsulates the download operation and its settings, allowing for controlled execution via a `run()` method.
4.  **Builder Pattern:** `DownloaderConfig.Builder` (and `FileDownloadSettingsBuilder`) provides a readable and flexible way to construct complex objects with multiple parameters.
5.  **Observer Pattern:** `AbstractDownloader` notifies `DownloadProgressObserver` instances of progress, completion, and errors, enabling loose coupling for reporting and event handling.
6.  **Strategy Pattern:** `FileDownloader` interface and its implementations (`ReliableDownloader`, `HTTPClientFileDownloader`) allow the client (`Program.java`) to interchange download algorithms based on settings.
7.  **Template Method Pattern:** `AbstractDownloader.downloadFile()` defines the overarching download algorithm (`beforeDownload()`, `performDownload()`, `afterDownload()`), allowing subclasses like `ReliableDownloader` to implement specific steps while maintaining a consistent flow.

These patterns collectively contribute to the project's modularity, flexibility, and extensibility, simplifying maintenance and future adaptations.

## Testing and Quality Assurance

Thorough testing was a critical component of this project's development, ensuring the robustness and reliability of the download mechanisms. Our testing strategy encompassed both comprehensive unit testing and targeted integration testing.

### Unit Testing

A strong emphasis was placed on **unit testing**, with individual components and functionalities being rigorously tested in isolation. I made extensive use of the provided `FakeCDN` implementation to simulate diverse download scenarios and edge cases. This allowed us to validate core logic, error handling, and state transitions efficiently and repeatably without reliance on external services.

### Integration and End-to-End Testing

For **integration and end-to-end testing**, I focused on real-world scenarios to validate the downloader's behavior under various conditions:

*   **Non-Resumable Downloads:** I thoroughly tested the application's ability to handle standard, non-resumable downloads using **endpoints from Accurx**. This ensured correct file transfer, progress tracking, and error handling for common download types.
*   **Resumable Downloads:** To validate the crucial resumable download feature, I utilized **Oracle JDK file download URLs**. These large, segmentable files provided an ideal test bed for verifying partial content requests, correct byte range handling, and seamless resumption of interrupted transfers.
*   **Network Resilience Testing with NetLimiter:** A key aspect of our testing involved simulating challenging network conditions. I leveraged the **NetLimiter network emulator** to introduce artificial latency, bandwidth throttling ("choking"), and packet loss. This allowed us to thoroughly test the application's resilience and, importantly, confirm that **resumed downloads function correctly even under severely degraded network performance**. This demonstrated the effectiveness of the retry and resumption mechanisms.

Through this multi-faceted testing approach, I gained high confidence in the application's stability, performance, and ability to handle a wide range of download scenarios reliably.

## Instructions on How to Build and Run (Gradle)

### Prerequisites

*   **Java Development Kit (JDK) 21** or higher
*   **Gradle 7.x** or higher (typically invoked via the `gradlew` wrapper)

### Build Instructions

Navigate to the project's root directory in your terminal and execute:
```bash
./gradlew clean build
```

### Running Tests
To run the project's tests, use the following command:
```bash
./gradlew clean test
```

### Running the Application
After a successful build, you can run the application using the Gradle `run` task. This command supports optional parameters for specifying the download URL, destination path, and network simulation conditions.
**Command:**

```bash
./gradlew run [OPTIONS]
```

**Options:**

*   `--url=<url_of_the_file_to_download>`: Specifies the URL of the file to download.
*   `--destination=<path_of_downloaded_file>`: Sets the local path where the downloaded file will be saved.
*   `--network=[good|poor]`: Simulates network conditions. Use `good` for optimal conditions or `poor` for throttled speeds.

**Example Full Command:**

```bash
.\gradlew run -PappArgs="--url=https://installer.accurx.com/chain/4.22.50587.0/accuRx.Installer.Local.msi --destination=./downloads/myfirstdownload.msi --network_conditions=poor"
```

## Default Configurations for Robustness on Slow Networks

The application is configured with sensible defaults to ensure reliable downloads, especially in environments with slow or intermittent network connectivity.

*   **`DEFAULT_CHUNK_SIZE = 65536` (64 KB):** This defines the size of data segments requested during a download. A moderate chunk size helps in situations with high packet loss or unstable connections by reducing the amount of data that needs to be re-transmitted if a segment fails.
*   **`DEFAULT_MAX_RETRIES = 5`:** Specifies the maximum number of times the system will attempt to retry a failed download segment or connection. This directly enhances resilience against transient network issues.
*   **`DEFAULT_RETRY_DELAY = Duration.ofSeconds(3)`:** Sets the initial delay before the first retry attempt. This delay increases exponentially with subsequent retries, providing the network a brief but progressively longer period to recover from congestion or instability. This exponential back-off strategy prevents overwhelming an already struggling network.
*   **`DEFAULT_BUFFER_SIZE = 65536` (64 KB):** This internal buffer size helps optimize data transfer by allowing the application to read/write data in larger blocks, which can be more efficient, particularly over networks with higher latency.
*   **`DEFAULT_CONNECT_TIMEOUT = Duration.ofMinutes(1)`:** The maximum time allowed to establish a connection to the download server. A longer timeout is beneficial on slow networks where initial connection handshake might take more time.
*   **`DEFAULT_READ_TIMEOUT = Duration.ofMinutes(5)`:** The maximum time allowed for data to be read from the established connection. A generous read timeout prevents premature disconnections due to temporary network stalls or very slow data transfer rates.

### Calculation of Retry Attempts and Time Duration

The retry mechanism employs an exponential back-off strategy. With `DEFAULT_MAX_RETRIES = 5` and an initial `DEFAULT_RETRY_DELAY = 3` seconds, the delays between retries would be approximately:

*   *Attempt 1 (Initial):* 0 seconds (first attempt)
*   *Attempt 2 (Retry 1):* 3 seconds delay
*   *Attempt 3 (Retry 2):* 6 seconds delay (3 * 2)
*   *Attempt 4 (Retry 3):* 12 seconds delay (3 * 2^2)
*   *Attempt 5 (Retry 4):* 24 seconds delay (3 * 2^3)
*   *Attempt 6 (Retry 5):* 48 seconds delay (3 * 2^4)
*   **Total : 93 seconds**

Overall, the application is configured to attempt recovery for a specified duration before ultimately ceasing its efforts. As these parameters are fully configurable, they can be fine-tuned to meet specific requirements and adapt to diverse real-world network conditions.