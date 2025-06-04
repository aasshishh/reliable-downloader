# Questions

## How did you approach solving the problem?
File downloading is a common problem with established solutions, often involving techniques such as chunking, progress monitoring, and data integrity verification (e.g., checksums).

Initially, as a developer with a background primarily in C++, I focused on setting up the Java development environment and validating basic functionality. Following this, I proceeded to implement the core components and integrate them. Once a functional solution was achieved, the focus shifted to enhancing code quality, which included implementing comprehensive tests and refactoring for modularity.

Throughout the development process, AI tools, particularly IntelliJ's integrated AI Chat functionality, were extensively utilized to overcome development hurdles. The following prompts illustrate how these tools facilitated progress:

1.  "As a C++ developer, assist me in setting up a Java project from scratch."
2.  "Provide guidance on resolving build errors and Java version incompatibility issues."
3.  "Generate more comprehensive test cases for this class."
4.  "Propose enhancements for the `ReliableDownloader` class, such as 'Robustness and Error Handling in `initializeDownload`' and 'State Management for Resume' capabilities."
5.  "Explain the functionality of `fakeCdn` and its application in developing more robust `ReliableDownloaderTests`."
6.  "Describe how to test a Java binary for slow and poor network conditions when downloading a file from the internet on a Windows environment."
   *Answer* : Use tools like NetLimiter
7.  "Assist in generating a project report."


Areas where AI was not utilized:

*   **Project Design:** This includes the architectural design and the implementation of specific design patterns.
*   **Core Functionality Implementation:** Specifically, the implementation of the resume download capability, which differentiates `HTTPClientFileDownloader` and `ReliableDownloader` was handled independently.
*   **Debugging and Test Failure Resolution:** The identification and correction of bugs, as well as addressing failing tests, were performed manually.

## How did you verify your solution works correctly?
The correctness of the solution was validated through the following methods:
*   **Unit Testing:** Comprehensive unit tests were developed and executed to ensure individual components functioned as expected.
*   **Manual End-to-End Testing:** The solution was manually tested against endpoints provided by Accurx and Oracle. Successful file downloads were verified for integrity and functionality.
*   **Simulated Network Conditions:** Network performance was intentionally degraded using NetLimiter to simulate poor network conditions. The application's resilience and behavior under these challenging environments were then thoroughly tested.

## How long did you spend on the exercise?
- Approx 2 days

## What would you add if you had more time and how?
To evolve this into a full-fledged production application, the primary focus would be on significantly enhancing its robustness, efficiency, and overall user experience. This can be achieved by transforming the current implementation into a dedicated server application, complemented by a user interface (UI).

The server component would expose a comprehensive set of APIs to facilitate remote management and interaction, including:

*   **Download Management APIs:** For initiating, pausing, resuming, canceling, and querying the status of downloads.
*   **Configuration APIs:** To allow dynamic configuration of all relevant parameters, such as download paths, concurrency limits, and network settings.
*   **Notification APIs:** To provide real-time updates on download status changes, completion, or error events.
*   **Enhanced Progress Indicator APIs:** Offering more granular and detailed progress information than currently available, potentially including metrics like speed, estimated time remaining, and individual part completion for multi-part downloads.

Beyond the architectural shift to a server-based model, several critical enhancements can be made to the current implementation to significantly improve its resilience and overall performance. These areas include:

*   **Optimized Multi-threaded Downloading:** To maximize download speed and efficiency, the system would be enhanced to support simultaneous connections for concurrent downloading of different file segments. This optimization would involve:
   *   **Sophisticated Thread Pool Management:** Implementing an intelligent thread pool to efficiently manage and allocate resources for multiple download streams.
   *   **Leveraging HTTP Byte Range Requests:** Utilizing the HTTP `Range` header to precisely request and retrieve specific portions of a file, enabling parallel downloads.
   *   **Dynamic Concurrency Adjustment:** Developing logic to dynamically adapt the number of concurrent connections based on real-time network conditions and server responsiveness, ensuring optimal performance without overburdening resources.

*   **Advanced Bandwidth Management and Prioritization:** To provide users with greater control and ensure efficient network utilization, features for bandwidth throttling and download prioritization would be integrated:
   *   **Granular Rate Limiting Implementation:** Introducing mechanisms to precisely control and limit the data transfer rate per second, preventing network saturation.
   *   **Intelligent Queueing and Prioritization Logic:** Establishing a robust system for managing download queues and assigning priorities, ensuring that critical downloads are completed expeditiously.

*   **Real-time Checksum Verification during Download:** To enhance data integrity and detect corruption proactively, checksum verification would be performed throughout the download process, rather than solely post-completion:
   *   **Stream-based Hashing:** Implementing a continuous hashing mechanism to calculate checksums for incoming data chunks as they are received.
   *   **Incremental Verification against Provided Hashes:** If the server offers checksums for individual file segments, these would be incrementally verified against the calculated hashes during the download, providing early detection of any data inconsistencies.