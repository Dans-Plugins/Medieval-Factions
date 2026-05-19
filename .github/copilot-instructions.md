# Medieval Factions — Copilot Instructions

## Stack
Kotlin · Bukkit/Paper API · Gradle (Shadow JAR) · Java 17 toolchain

## Build
`./gradlew shadowJar`. CI builds Ponder from source (`Dans-Plugins/Ponder` tag `2.0.0`)
via `publishToMavenLocal` before the main build.

## DPC API integration (`dpc/MfDpcApiService`)
- Schema: `docs/dpc-api-schema.asn1`
- Field limits: `name` 64 · `serverId` 64 · `description` 512 · `serverIp` 253 · `discordLink` 512
- Truncate every field before adding to JSON; use the `truncate()` helper.
- `discordLink` must start with `https://discord.gg/` or `https://discord.com/`; validate at command time **and** in the service before including in the payload.
- `memberCount` must be a non-negative integer (`maxOf(0, …)`).
- `HttpClient` is injected for testability — tests mock it, no real network calls.
- `description` is OPTIONAL in the schema; the server-side API accepts null.

## Contract Tests (Pact)

Consumer-driven contract tests live in `MfDpcApiPactConsumerTest` (this repo) and
`DpcApiPactProviderTest` (dansplugins-dot-com). They cover the
`POST /api/v1/factions` wire format at the HTTP level without a real Minecraft server.

- **Consumer** (`src/test/kotlin/…/dpc/MfDpcApiPactConsumerTest.kt`): uses
  `au.com.dius.pact.consumer:junit5:4.6.7`. Builds a `DpcFactionPayload` list with Gson
  and sends it to a Pact mock server. Generates pact files to `pacts/` (configured via
  `pact.rootDir` in `build.gradle`).
- **Committed pact file**: `pacts/medieval-factions-dpc-api.json` — committed to both
  repos so the provider can verify without running the consumer test first.
- **Provider** (`DpcApiPactProviderTest` in dansplugins-dot-com): reads the committed pact
  file, starts Spring Boot on a random port (H2 in-memory, `@ActiveProfiles("test")`),
  `@MockBean ApiKeyService` accepting `"test-api-key"`, and verifies each interaction via
  `PactVerificationInvocationContextProvider`.

## Integration Testing Path Forward
The unit tests mock `HttpClient` and verify the JSON payload shape. For end-to-end
confidence, the next step is OMCSI-based integration tests (see
`.github/workflows/integration.yml` in `Dans-Plugin-Manager` for the pattern):

1. Build the MF shadow JAR.
2. Spin up a real Minecraft server via OMCSI.
3. Start a local mock DPC API (e.g. WireMock or a TestContainers-backed instance of
   `dpc-api` from the `dansplugins-dot-com` repo).
4. Set `dpc-api.enabled=true`, `dpc-api.key=<test-key>`, `dpc-api.server-id=ci-server`
   via RCON or config injection.
5. Wait one sync interval and assert the mock received a well-formed POST.

Until OMCSI tests exist, keep the mock-`HttpClient` tests comprehensive for all edge
cases (truncation, omitted fields, validation failures, skip-when-misconfigured).

## Conventions
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
