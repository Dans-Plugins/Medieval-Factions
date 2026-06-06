# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [Unreleased]

### Added
- `/faction declinevassalization [faction]` command (permission `mf.declinevassalization`, default `true`): lets a faction decline a pending vassalization request sent to it, with notifications to both factions.
- DPC community API integration: opt-in sync of faction data to `https://dansplugins.com` via `POST /api/v1/factions`. Enabled with `/mf dpc optin` and configured under the `dpc-api.*` section of `config.yml`. Requires an API key from the DPC website.
- `/mf dpc` subcommand (permission `mf.dpc`, default `op`) with `optin`, `optout`, `reminder on|off`, `shareip on|off`, `discord <link>|clear` actions.
- bStats charts for DPC opt-in rate, login-reminder usage, server-IP sharing, and Discord-link presence.

### Fixed
- Faction snapshot for the DPC sync is now collected on the Bukkit main thread before being dispatched off-thread via `HttpClient.sendAsync`. Off-thread access to `factionService.factions` could otherwise produce inconsistent reads or `ConcurrentModificationException` under load.
- The DPC sync no longer POSTs an empty faction roster. A transient empty read (e.g. faction data not yet loaded at startup, or a reload mid-cycle) is skipped client-side rather than sent, so it can never depend on the provider's safety guards to avoid a faction wipe.

## [5.8.1] – 2026-04-25

### Fixed
- Holding a `wartimePlaceableBlocks` item (e.g. ladders or scaffolding) no longer bypasses interaction protection on enemy territory blocks such as chests and levers during wartime.

## [5.8.0] – 2026-04-25

### Added
- `factions.wartimePlaceableBlocks`: configurable list of block types that attackers can place in enemy territory during war.
- `factions.wartimeBreakableBlocks`: configurable list of block types that attackers can break in enemy territory during war.
- `factions.wartimeInteractableBlocks`: configurable list of block types that attackers can interact with in enemy territory during war.

## [5.7.2] – 2026-01-03

### Fixed
- `nonMembersCanInteractWithDoors` configuration option not functioning as intended.
- Ladder bypass exploit allowing unintended access to protected areas.

## [5.7.1] – 2026-01-02

### Fixed
- Ladder placement incorrectly blocked in enemy territory during wartime.

## [5.7.0] – 2026-01-01

### Added
- Leaderless faction support with operator management commands.
- World-based claim blocking configuration options.
- Automated JAR publishing to GitHub Packages on release.
- Comprehensive in-repository documentation.

### Fixed
- Double-chest hopper bypass and other protection edge cases.
- NullPointerExceptions affecting plugin stability.
- Concurrent gate save handling.

### Changed
- Test server updated to Minecraft 1.21.11.
- Improved faction flag commands with force permissions and refactoring.
- Removed outdated territory item pickup/drop restrictions.

## [5.6.1] – 2025-12-09

### Fixed
- `NoSuchElementException` during plugin initialization when player data is unavailable.
- `/mf bypass` not allowing players to attack entities in claimed chunks.
- Infinite recursion in the faction claim fill command (added recursion depth cap).
- Lock command not persisting across multiple blocks like the unlock command.
- Slimefun compatibility: added comprehensive event listeners to prevent bypassing faction protection.
- Entity protection in faction territories not respecting relationships.
- Dynmap integration causing lag on server and web interface.

## [5.6.0] – 2025-03-30

### Added
- Configurable gate block restrictions with an expanded default list.
- Config options to restrict block actions in unclaimed wilderness chunks.

### Fixed
- Ally placeholder issue.
- Gate blocks being destroyed by fire.

### Changed
- Improved GitHub issue templates for clarity and consistency.
- Enabled PlaceholderAPI testing.

## [5.5.0] – 2025-03-13

### Added
- Unit tests for Dynmap integration.

### Fixed
- Anvil duplication exploit involving falling blocks in gates.

### Changed
- Dynmap processing made more configurable (optional realm and faction info display).
- Dynmap now reflects faction disbandment.
- Test server updated to Minecraft 1.21.4.
- Simplified test server setup.

## [5.4.0] – 2025-03-02

### Added
- Ability for players to submit applications to join factions.
- Dockerfile with Dynmap support.

### Fixed
- Power insufficiency check failing when a faction attempted to conquer land.

## [5.3.0] – 2024-01-19

### Added
- Brazilian Portuguese (pt-BR) translation.
- Config option to only render territories upon startup.
- Config option for claim fill max chunks.
- Docker-based test server.

### Fixed
- Disabling neutrality preventing the plugin from enabling.

### Removed
- Old claim commands (Phase 3 deprecation).

## [5.2.0] – 2023-07-06

### Added
- `protectVillagerTrade` faction flag.
- `factions.maxMembers` config option.
- `players.minPower` config option.
- Toggle Dynmap integration config option.
- Toggle block destruction in wartime config option.
- Expanded territory title notifications.

### Removed
- Old claim commands (Phase 2 deprecation).
- Chat preview listener.

## [5.1.4] – 2023-05-24

### Added
- Unique name check to `set name` command.
- Permission check for `mf claim auto` command.

### Fixed
- Players stealing power upon killing a player even when the victim had no power to steal.
- Language resource bundles only included if they exist.
