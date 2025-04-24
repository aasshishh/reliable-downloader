# 👩‍⚕️ Reliable Downloader Exercise 🖥️

[Find out more about Accurx](https://www.accurx.com/careers)

Thank you for investing your time in our take-home exercise.

We've based this exercise on a real problem we've had to solve to be able to release our desktop software, which allows clinicians to communicate with patients over text, video, questionnaires, and other methods to hundreds of thousands of users multiple times a week.

### Why have we set this exercise?
So we get an insight into how you:
* Understand and  analyse requirements to solve real user problems
* Utilise language features, structure code and tests to verify your solution meets user requirements. 

### What is expected of you?
 **Please complete the task detailed below**👇

**Please answer the following questions in `questions.md`**
   - How did you approach solving the problem?
   - How did you verify your solution works correctly?
   - How long did you spend on the exercise?
   - What would you add if you had more time and how?
     
 **When you're finished please:**
   - Download your solution including your completed `questions.md` file (in GitHub, at the route of your repository, click Code -> Download Zip)
   - Submit your zipped solution, using the link in your invite email.

_Please feel free to add any feedback you have on this exercise in the submission `feedback.md`_

## Task
### Context
The component that clinicians use for downloading updates needs to be reliable in the unreliable network conditions 
they work with; often facing intermittent internet disconnection and slow internet speeds.

We've implemented the full download and would like you to extend it (you can choose the .NET or Java project) such that
the program should not terminate until the download has been completed successfully. This means that it should be
resilient to:
- Internet disconnections of any length (from a couple of seconds to over two minutes) 
- Partial downloading. So that the download doesn't need to start from scratch every time if the CDN [supports this](https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Accept-Ranges).

### What does a successful submission look like?
A high-quality submission will have the following:
- Meet the requirements above
- Appropriate use of language features to solve the problem in a simple way
- Code that is easy to read and reason about
- Unit tests

## Tips

- Take the time to read through the task and description. There's guidance in there that can be helpful to approaching the problem
- Try writing down some example inputs and outputs on paper
- Try a brute force approach and then optimise the code
- Add some comments to your code if you think it will be helpful to share your thought process to someone assessing it
- You can throttle your internet connection using NetLimiter or similar
- You can simulate internet disconnections by disconnecting wifi/ethernet
- Different behaviours occur after following different periods of disconnection, two seconds and two minutes are sweet spots for exercising key failure modes
- The use of third-party libraries is fine, however, do bear in mind that overusing these may limit your ability to demonstrate your skills
