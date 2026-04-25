# Copilot Instructions

This repository follows the DPC (Dans Plugins Community) conventions defined at
https://github.com/Dans-Plugins/dpc-conventions. Read those conventions before
making any changes.

## Technology Stack

- Language: Kotlin (JVM)
- Build tool: Gradle (Groovy DSL) with the Shadow plugin for fat JAR builds
- Target platform: Spigot / Paper (Minecraft plugin, API version 1.17+)
- Test framework: JUnit 5
- Database ORM: JOOQ with Flyway migrations; supports H2, MySQL, and PostgreSQL
- Connection pooling: HikariCP

## Project Structure

- `src/main/kotlin/` – Plugin source code (Kotlin)
- `src/main/resources/` – `plugin.yml`, `config.yml`, Flyway migration scripts, and `lang/` language files
- `src/test/kotlin/` – Unit tests
- `.github/workflows/` – CI and release workflows
- `compose.yml` – Docker Compose configuration for the local development server
- `up.sh` / `down.sh` / `reload-plugin.sh` – Helper scripts for the Docker dev server

## Coding Conventions

- Use the `lang/` resource files for every user-facing string; never hard-code messages in Kotlin or Java.
- Follow the existing package structure under `com.dansplugins.factionsystem` when adding new classes.
- Commands are registered and handled via the existing command system in the `command/` package.
- Event listeners live in the `listener/` package and should implement `Listener`.
- All database access goes through the service classes in the respective domain packages (e.g. `faction/`, `claim/`, `player/`).
- New configuration options must be documented in `CONFIG.md`.
- New commands must be documented in `COMMANDS.md`.
- Documentation changes that accompany a code change should be included in the same pull request.

## Contribution Workflow

- Branch from `develop` for all changes.
- Open a pull request against `develop`, not `main`.
- Reference the related GitHub issue in every pull request description using `#<number>`.
- The CI build (`.github/workflows/build.yml`) must pass before a pull request can be merged.
