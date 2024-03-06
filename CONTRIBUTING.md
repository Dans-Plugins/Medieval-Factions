# Contributing Document
## Thank You
Thank you for being interested in contributing to the project! It wouldn't be where it is today without the help of the community. This document will help you get started with contributing to the project.

## Links
- [Website](https://dansplugins.com)
- [Discord](https://discord.gg/xXtuAQ2)

## Requirements
- A GitHub account
- Git installed on your local machine
- A text editor or IDE
- A basic understanding of Java

## Getting Started
- If you don't already have a GitHub account, you can sign up for one [here](https://github.com/signup).
- Fork the repository on GitHub by clicking the "Fork" button on the top right of the repository page.
- Clone your fork of the repository to your local machine using `git clone https://www.github.com/<your-username>/Medieval-Factions.git`
- Open the project in your preferred text editor or IDE.
- Try compiling the plugin using the following command:
  ```bash
  gradlew build
  ```
  If you encounter any errors, please create an issue for it.

## Identifying What To Work On
### Issues
Work items are tracked as GitHub issues. You can find a full list of issues [here](https://github.com/Dans-Plugins/Medieval-Factions/issues).

### Milestones
Work items are organized into milestones, which represent a specific version of the plugin. You can find the milestones [here](https://github.com/Dans-Plugins/Medieval-Factions/milestones).

## Making Changes
- Before you start working on something, make sure there is an issue for it. If there isn't, create one.
- Make sure you are working on the "develop" branch. If you are not, switch to it using `git checkout develop`.
- Create a new branch for your changes using `git checkout -b <branch-name>`. Make sure to name your branch something that is related to the issue you are working on.
- Make your changes to the code.
- Test your changes to make sure they work as expected. [More information on testing can be found here](#testing).
- When you are finished, commit your changes using `git commit -m "Your commit message here"`.
- Push your changes to your fork using `git push origin <branch-name>`.
- Open a pull request on the original repository. Make sure to include a description of your changes and link the related issue using #(number). The develop branch should be used as the base branch.
- Wait for your pull request to be reviewed. If there are any changes that need to be made, make them and push the changes to your fork. Your pull request will be updated automatically.
- Once your pull request has been reviewed and approved, it will be merged into the develop branch.

### Language Files
The plugin supports multiple languages. Code changes that require changes to the language files should include the changes to the language files as well. If you are adding a new language, you will need to create a new language file. The language files are located in the `src/main/resources/lang` directory.

## Testing
At this time, there are no unit tests due to a difficulty mocking Spigot. However, you can test your changes by running the plugin on a Spigot server.

### Running a Spigot server with Docker
If you don't have Docker installed, you can download it [here](https://www.docker.com/products/docker-desktop).

To run a Spigot server with Docker, you can use the following command:
```bash
docker compose up
```

This will start a Spigot server on your local machine. You can connect to it using the IP `localhost` and the port `25565`.

If you make changes to the code, you can deploy the latest changes by rebuilding the Docker image:
```bash
docker compose up --build
```

## Questions
If you have any questions about contributing to the project, feel free to ask in the Discord server. You can join the Discord server [here](https://discord.gg/xXtuAQ2). This is the best place to ask questions.