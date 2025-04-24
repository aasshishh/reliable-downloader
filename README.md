# 👩‍⚕️ Reliable Downloader Exercise 🖥️

[Find out more about Accurx](https://www.accurx.com/careers)

Thank you for investing your time in our take-home exercise.

We've based this exercise on a real problem we've had to solve to be able to release our desktop software, which allows clinicians to communicate with patients over text, video, questionnaires, and other methods to hundreds of thousands of users multiple times a week.

## Why have we set this exercise?

So we get an insight into how you:

- Understand and analyse requirements to solve real user problems
- Utilise language features, structure code and tests to verify your solution meets user requirements

## What is expected of you?

Please complete 'The Task' detailed below 👇 (you can choose to implement in [C#](./dot-net/ReliableDownloader.sln) or [Java](./java/)) and then answer the following questions in [questions.md](./questions.md)

1. How did you approach solving the problem?
  - _if you used AI assistance we'd love to see examples of your prompts_
2. How did you verify your solution works correctly?
3. How long did you spend on the exercise?
4. If you had more time what would you add and how?

We'd welcome any feedback you have on this exercise in [feedback.md](./feedback.md)

**When you're finished please:**

1. Download your solution, ensuring you include your completed [questions.md](./questions.md) file (in GitHub, at the root of your repository, click Code -> Download Zip)
2. Submit your zipped solution, using the link in your invite email
   _(please avoid including your name/email in the zip filename)_

## The Task

Clinicans are faced with unreliable network conditions including intermittent disconnection and low bandwidth.  The updater they use for downloading the latest updates to our desktop app needs to work reliably in these challenging conditions.

We've provided a basic implementation of the updater that downloads a file from a URL to disk and verifies its integrity.  Your challenge is to extend it to:

1. be resilient to network disconnections of _any_ length (from a couple of seconds to over two minutes)
2. where the CDN supports [range requests](https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Accept-Ranges), download the file in chunks and pick up where it left off if the user restarts the installer

### What does a successful submission look like?

A high-quality submission will:

- meet the two requirements above
- make appropriate use of modern language features to solve the problem in an elegant way
- be easy to read and reason about
- have unit tests around the range requests implementation

We're not expecting unit tests around the resilience mechanism but we will be looking for evidience it has been manually tested by running it ourselves and reviewing your answer to question #2 in [questions.md](./questions.md).

### Tips

- Take the time to read through the task and description. There's guidance in there that can be helpful to approaching the problem
- Try writing down some example inputs and outputs on paper
- Try a brute force approach and then optimise the code
- Add some comments to your code if you think it will be helpful to share your thought process to someone assessing it
- You can throttle your internet connection using NetLimiter or similar
- You can simulate internet disconnections by disconnecting wifi/ethernet
- Different behaviours occur after following different periods of disconnection, two seconds and two minutes are sweet spots for exercising key failure modes
- The use of third-party libraries is fine, however, do bear in mind that overusing these may limit your ability to demonstrate your skills
