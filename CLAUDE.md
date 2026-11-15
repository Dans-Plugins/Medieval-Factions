# CLAUDE.md — Medieval Factions

## What this repo is

Medieval Factions is a Spigot/Paper plugin, written in Kotlin, that lets players form factions, claim land, declare war, make alliances and vassals, and keep laws. It runs on live community servers, so faction, claim, player and power records are irreplaceable data. The jar built from this repo (`./gradlew shadowJar`) is the product.

Stack: Kotlin (JVM, Java 17 toolchain), Gradle (Groovy DSL) with the Shadow plugin, JUnit 5, jOOQ + Flyway on H2 (default), MySQL/MariaDB or PostgreSQL, HikariCP, and an alternative JSON file backend (`storage.type: json`). `.github/copilot-instructions.md` holds the same stack and structure notes for GitHub Copilot, plus the DPC API wire contract.

## Conventions

These follow the [DPC conventions](https://github.com/Dans-Plugins/dpc-conventions). Repo-specific rules:

- **Data safety comes first.** Existing servers must never lose data or change behaviour because of an upgrade.
  - Flyway migrations (`src/main/resources/com/dansplugins/factionsystem/db/migration/V<n>__*.sql`) are append-only and additive. Never edit a released migration; never `DROP`, `TRUNCATE`, `DELETE`, narrow a type, or add `NOT NULL` without a default.
  - Do not change a default in `config.yml`. A new option ships off, or with a default that keeps today's behaviour, and the code-side fallback (`config.getBoolean("<key>", <def>)`) must equal the shipped default.
  - The DPC sync (`dpc/MfDpcApiService`) must never send an empty or partial faction set. See `.github/copilot-instructions.md` for its field limits.
- **Every user-facing string goes through `src/main/resources/lang/`.** A new key is added to `lang_en_US` and `lang_en_GB`, and to `lang_de_DE`, `lang_fr_FR` and `lang_pt_BR` where a translation is available. The files do not share one encoding: `lang_de_DE` is ISO-8859-1, `lang_fr_FR` and `lang_pt_BR` are UTF-8, the English files are ASCII. Check with `file -b <path>` and write bytes in that file's own encoding; `./check-lang-encoding.sh` checks the result.
- **Storage goes through the domain services** (`faction/`, `claim/`, `player/`, `law/`, …), never ad-hoc SQL. Both backends (jOOQ and JSON) implement each repository interface, and a behaviour change to one usually needs the same change in the other.
- **Commands** are handled in `command/` and declared, with every permission node, in `src/main/resources/plugin.yml`. **Listeners** live in `listener/`.
- **Java API compatibility:** a public function whose signature takes or returns an `@JvmInline value class` ID (`MfFactionId`, `MfPlayerId`, …) needs `@JvmName`, and an existing `@JvmName` is never removed.
- **Bukkit calls stay on the main thread.** Commands do their database work in `runTaskAsynchronously` and hop back with `runTask` for world access; read a player's position or health before going async.
- A new config option is documented in `CONFIG.md`, a new command or permission in `COMMANDS.md`, a new faction flag in `FACTION_FLAGS.md`, and every user-visible change in `CHANGELOG.md` under `[Unreleased]`, in the same pull request.

## What belongs here vs. elsewhere

Medieval Factions is the core of a group of plugins. Change this repo only for the core plugin, the services and events add-ons build on, and the opt-in REST API (`api/`, documented in `API_USAGE.md`).

- Add-ons have their own repos and depend on this one: Fiefs, Currencies, and the map integrations such as `Bluemap_MedievalFactions`. A change an add-on needs from the core lands here first, as an addition that keeps existing add-ons working.
- The behaviour table that `BEHAVIOUR.md` is generated from lives in [Dans-Plugins/plugin-fixtures](https://github.com/Dans-Plugins/plugin-fixtures) (`scenarios/medieval-factions-behaviour.json`), and the gate that runs it in [Dans-Plugins/release-gates](https://github.com/Dans-Plugins/release-gates). Do not hand-edit the table between the `behaviour-table` markers in `BEHAVIOUR.md`.
- Releases and version bumps are made by the release automation (`release/*` and `chore/*` pull requests), not by hand.
- The DPC API's side of the faction sync lives in [Dans-Plugins/dansplugins-dot-com](https://github.com/Dans-Plugins/dansplugins-dot-com); the Pact file `pacts/medieval-factions-dpc-api.json` is shared with it.

## Grounding design decisions

There is no research document. Ground a change in the issue it resolves, in the existing code it extends (match the nearest sibling command, listener or service), and in the user-facing docs listed below. A claim that a behaviour gap exists, or that a change closes one, is either reproduced in game (on a local server, with a control case by a player who is allowed to do the action) or worded as unverified.

## Testing changes

```bash
./gradlew clean test     # JUnit 5 unit tests
./gradlew lintKotlin     # kotlinter; ./gradlew formatKotlin fixes most findings
./gradlew clean build    # what CI runs, then the Bukkit API compatibility check
```

- Tests live in `src/test/kotlin/com/dansplugins/factionsystem/<domain>/`, named `<ClassUnderTest>Test.kt`, and mock collaborators: no real network or database.
- A bug fix comes with a regression test that fails without the fix.
- A change to the DPC wire format updates the Pact consumer test (`dpc/MfDpcApiPactConsumerTest.kt`) and the committed pact file.
- A change to what a player can do in the world (protection, locks, PvP, refusal messages) is checked on a server: `docker compose up` (see `CONTRIBUTING.md`) or any local Spigot server. The behaviour gate (`.github/workflows/behaviour.yml`) replays the plugin-fixtures table against the change on pull requests that touch protection code.
- Pull requests that change only markdown skip the Gradle and Docker builds; the `build` and `docker-build` checks still report.

## Commit and PR conventions

- **Branch from `main`** (the old `develop` branch is retired). Prefix the branch with its kind: `feature/` for a new capability, `fix/` for a correction, `docs/` for documentation only, `chore/` for upkeep. Name the rest after the change, e.g. `fix/2129-law-move-validation`. (`release/*`, `chore/next-snapshot-*` and `dev-loop/*` branches are made by automation.)
- **Commit messages** are in the imperative mood with no trailing period: "Add law pagination", not "Added law pagination."
- **Agent-authored commits** end with a `Co-Authored-By:` trailer naming the model that wrote them, on its own line after a blank line. Write the message with a HEREDOC or a message file (`git commit -F <file>`) so the trailer is not mangled by shell quoting. Human-authored commits carry no such trailer.
- **Pull requests** target `main`, and the body says `Closes #N` for each issue it resolves, so the issue closes on merge. Check that each `#N` is the issue the pull request actually resolves before opening it.
- **Merging:** pull requests are merged with a merge commit ("Merge pull request #N from …"); squash and rebase merging are turned off for this repo, and the branch is deleted on merge. The `build` and `docker-build` checks must pass first.

## Documentation sources of truth

| File | What to verify against the code |
|---|---|
| `README.md` | Install and upgrade steps, supported versions (`minecraft-versions.json`), storage options and links still hold |
| `COMMANDS.md` | Every command, usage, alias and permission node matches the `command/` handlers and `plugin.yml` |
| `CONFIG.md` | Every key and default matches `src/main/resources/config.yml` and the code-side fallbacks |
| `FACTION_FLAGS.md` | Every flag, type and default matches the flag definitions (`faction/flag/`) |
| `USER_GUIDE.md` | Described gameplay (claims, power, war, laws, locks, gates, duels) matches what the code does |
| `FAQ.md` | Answers still match current behaviour and config |
| `BEHAVIOUR.md` | The prose outside the generated table matches the plugin-fixtures table; the table itself is regenerated by the release automation |
| `API_USAGE.md` | The REST API endpoints, config keys and payloads match `api/` and `config.yml` |
| `DATABASE_QUERYING.md` | Table and column names match the Flyway migrations |
| `DEPENDENCY_USAGE.md` | The coordinates and publishing steps for depending on the plugin jar match `build.gradle` and the release workflow |
| `CHANGELOG.md` | Every user-visible change since the last release is under `[Unreleased]` |
| `CONTRIBUTING.md` | Build, test and branch instructions match this file and the workflows |
| `.github/copilot-instructions.md` | Stack, structure and the DPC contract match the code; conventions agree with this file |
