# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/).

## [Unreleased]

### Added
- A JSON file storage backend as an alternative to the database, selected with `storage.type: json` in `config.yml`. The default remains `database`, so existing servers are unaffected. Files are written to `storage.json.path`, one per entity type. `players.json` and `factions.json` are validated against a JSON schema before being written; the other entity files are not yet schema-validated, and no file is validated when read back. Intended for smaller servers that want simpler deployment and file-level backups — the database backend remains the better choice under load, since every JSON write rewrites the whole file for that entity type.
- `/faction migrate toJson` and `/faction migrate toDatabase` (permission `mf.migrate`, default `op`), which copy all data between the two storage backends. The migration runs asynchronously; `storage.type` must be changed and the server restarted afterwards for the new backend to take effect. The target backend must be empty — the command refuses to migrate into one that already holds data — and it should be run with no players online, since changes made while it runs may not be carried over. When migrating to JSON, only the most recent 1000 chat messages per faction are retained. See `docs/MIGRATION_GUIDE.md`.
- A rolling `dev` prerelease, rebuilt and republished automatically on every push to `main` that touches something other than documentation. It carries the shadow JAR built from the current `main`, so unreleased changes can be picked up without waiting for a release. It is installable with Dan's Plugin Manager via `/dpm get medievalfactions --experimental`, and `/dpm get medievalfactions --stable` returns to published releases. Dev builds are unreleased, unreviewed code and are not suitable for a server that matters.
- `FAQ.md`, a committed FAQ document alongside the existing guides. The first section covers disbanding: how self-disband works, and that `/faction disband <faction name>` (permission `mf.disband.others`, default `op`) force-disbands any faction, bypassing both the `DISBAND` role permission check and the last-member-only requirement, with no confirmation prompt. Linked from `README.md` and `USER_GUIDE.md`.
- Chat formats now resolve PlaceholderAPI placeholders belonging to other plugins. A placeholder such as `%someplugin_some_placeholder%` written into `chat.faction.format`, `chat.vassals.format` or `chat.allies.format` is replaced with its value for the player sending the message, rather than being shown verbatim. Only the configured format itself is resolved: a `%...%` sequence typed into a chat message, or carried in a display name set by a nickname plugin, is never expanded, so players cannot read placeholders through chat. Colour codes are translated last, so a placeholder that emits `&`-codes is coloured as expected. PlaceholderAPI stays optional: when it is not installed, formats behave exactly as before. The default (non-channel) chat format is unchanged, as it is hardcoded and contains no placeholders to resolve.

### Changed
- `/faction addmember` no longer silently refuses when the target player is already in another faction. It now prompts the admin for confirmation before removing the player from their current faction and adding them to the target; the `-f` flag skips the prompt and moves the player immediately. Both factions are notified of the move.

### Fixed
- The default `dpc-api.url` pointed at `https://dansplugins.com/api/v1/factions`, which is the community website rather than the API. The website answers an unknown `/api/...` path with its own HTML 404 page, so a server that opted in without overriding the URL logged `DPC API returned status 404` on every sync and never registered a faction. The default is now `https://api.dansplugins.com/api/v1/factions`. Servers that set `dpc-api.url` explicitly are unaffected, but a server relying on the old default needs the new value copied into its existing `config.yml`, since an existing file is not rewritten on upgrade. A server with no factions could not observe this at all: an empty roster is never sent, so the URL was never exercised.
- The `Dev Release` workflow now retries publishing the `dev` prerelease before giving up, and asserts that exactly one JAR is being published. The release and its tag have to be deleted and recreated for the tag to move to the new commit, and a transient API failure inside that window previously left the repository with no `dev` release at all until the workflow was re-run by hand.
- Disconnecting no longer performs a blocking database write on the server thread. Every quit saved the player synchronously from `PlayerQuitListener`, running a jOOQ upsert and the follow-up read it does to return the new row on the main thread; on a live server this was observed blocking that thread long enough for Spigot's 60-second watchdog to terminate the server. The write is now dispatched with `runTaskAsynchronously`, which is what every other listener in the plugin already does — the quit listener was the only one still writing to the database on the server thread. The player is snapshotted on the server thread before dispatching, so `powerAtLogout` still records the power held at the moment of the quit rather than whatever is cached by the time the write runs. `unloadInteractionStatus` is deliberately kept on the server thread as well: it only evicts an in-memory entry and does not depend on the save having completed, and leaving it there keeps it ordered ahead of the `loadInteractionStatus` that a reconnect performs during pre-login, which a deferred unload could otherwise have undone for a player who was back online. One behaviour change follows from that: the interaction status is now unloaded even when the save fails, where previously a failed save left it loaded for a player who was no longer on the server.
- `/faction unclaim`, `/faction gate remove` and `/faction claim check` now act on where the player stood when the command was run, rather than on wherever they had walked to by the time the queued lookup ran. All three read the sender's position from inside an asynchronous task, so a player who kept moving could unclaim a chunk they had already left, destroy a gate measured from the wrong place, or be told about a neighbouring faction's claim. Reading a position off the server thread is also unsafe in its own right — Bukkit mutates it in place, so coordinates read from another thread can mix pre-move and post-move values, and resolving a chunk or a block from one can reach into chunk loading. Each command now takes its position on the main thread before dispatching, matching what `/faction sethome` and `/faction map` already did. `/faction unclaim` no longer loads the chunks in its radius either: the chunks to unclaim are worked out arithmetically from the sender's chunk coordinates, which is what its claim lookups were keyed on all along, so the main-thread hop it made purely to call `getChunkAt` is gone. Which claims, gates and messages result is otherwise unchanged.
- `/faction claim circle`, `/faction claim fill` and `/duel accept` now act on where the players stood when the command was run, rather than on wherever they had walked to by the time the queued task ran. Each read the sender's position from inside an asynchronous task, so a player who kept moving could claim a circle centred on the wrong chunk, flood-fill from the wrong chunk, or have a duel record a return position they had already left. Reading a position off the server thread is also unsafe in its own right — Bukkit mutates it in place, so coordinates read from another thread can mix pre-move and post-move values, and resolving a chunk from one can reach into chunk loading. Each command now takes its position on the main thread before dispatching, matching what `/faction sethome`, `/faction map`, `/faction unclaim`, `/faction gate remove` and `/faction claim check` already did. The two claim commands also took the sender's world off-thread when deciding whether claiming is blocked there, and now take that on the main thread too. Which chunks are claimed, and which messages result, is otherwise unchanged.
- `/faction map` now renders the world the player stood in when the command was run. The chunk coordinates the map is centred on were already taken on the main thread before the lookup was queued, but the world was read from inside the queued task, so a player who changed world in between was shown a grid of the world they arrived in indexed by coordinates from the world they left — claims that are nowhere near where they are standing. Reading the world off the server thread is also unsafe in its own right, and the reference was passed into a claim lookup for every one of the grid's 189 cells. The world is now taken from the same `Location` read that already supplied the coordinates, so the two are always consistent with one another. Which claims a stationary player sees is unchanged.
- A JSON storage file whose contents could not be read is no longer replaced with an empty one. Every JSON repository loads the whole file, changes the loaded copy and writes it back, so a read that failed and was reported as "the file is empty" was one write away from replacing real records with nothing — and only two of the twelve entity files copied the unreadable contents aside first. All twelve now share one load path: the contents are copied to `<name>.corrupted.backup` (an existing backup is kept, and a later failure is copied to `<name>.corrupted.<timestamp>.backup`), the failure is logged at `SEVERE`, and writes to that file are refused until it can be read again. Reads return nothing meanwhile, so the rest of the plugin keeps running, and repairing or removing the file restores writes with no restart. An absent or empty file is unaffected — that is what a fresh install looks like, and it is written to normally. Only the JSON backend is affected; the database backend never had this behaviour.
- Right-clicking an entity in another faction's claim is now checked for every entity type, not just villagers. `PlayerInteractEntityListener` guarded villagers alone, leaving minecarts, boats, item frames, animals and the like to `PlayerInteractAtEntityListener`, whose coverage depends on which of the two Bukkit interact-entity events a given server version raises for that entity — and chest and hopper minecarts and item frames make that a theft vector rather than a cosmetic gap. Both listeners now share the same claim check, wilderness included, so a player without interaction rights is blocked whichever event the interaction arrives on. The new `factions.nonMembersCanInteractWithEntities` option (default `false`, that is, protected) turns the general entity check off for servers that want entities left interactive. Setting it to `true` is not a return to the previous behaviour: it is more permissive than that, because it also gives up the armour-stand protection that the `AtEntity` path already provided before this change. Villager trading is unchanged: it remains gated on the `protectVillagerTrade` faction flag and is deliberately left out of the general check, so a faction that has turned that flag off can still be traded with. A single right-click can raise both events, so the two listeners also share a short de-duplication window and the player is told only once.
- Standing on a *locked* pressure plate (or tripwire, or farmland) you are not allowed to use no longer floods chat with a lock message every tick. This covers the one path missed by the previous physical-interaction fix, and it also stops the per-tick scheduled task and block-owner lookup that each of those messages performed. The lock protection itself is unchanged — the interaction is still blocked.
- A player with no `MfPlayer` record yet who stands on a protected physical block (pressure plate, tripwire, farmland) no longer queues a duplicate async player-save every tick. Only the first physical interaction dispatches a save; further ticks are ignored until that save completes.
- Drinking is no longer blocked while looking at a protected block. The existing exemption that lets a player eat while facing another faction's claim only covered items Bukkit reports as edible, which excludes potions and milk buckets, so drinking one in a claim you cannot interact with was cancelled. Potions, milk buckets and honey bottles are now exempt alongside food.
- Eating or drinking in wilderness is no longer blocked when `wilderness.interaction.prevent` is enabled. That option is there to protect blocks, and consuming an item does not touch the block being looked at.
- Holding food no longer exempts a `LEFT_CLICK_BLOCK` from interaction protection in a claim. A left-click never consumes an item, so the exemption is now limited to right-clicks, closing a small protection gap.
- Every other hand-used item is no longer blocked while looking at a protected block. Drawing a bow or a crossbow, throwing a snowball, an egg, an ender pearl, a trident or a splash potion, casting a fishing rod, raising a shield, using a spyglass or a goat horn were all still cancelled in another faction's claim, in wilderness with `wilderness.interaction.prevent` enabled, and at a locked block, because the exemption was a list of consumable materials rather than a statement about what the interaction may do. Interaction protection now refuses only the block half of the interaction, leaving the item in hand alone whenever that item's use cannot place, break or alter a block. Protection itself is unchanged: chests, levers, doors, buttons and cauldrons stay shut whatever is held; left-clicks and physical interactions are still refused outright; and an item that does act on the clicked block — a bucket, flint and steel, a hoe, an axe, a shovel, bone meal, an eye of ender, a firework rocket, a wind charge, or anything placeable — is still refused outright too.
- Breaking a block in enemy territory (`LEFT_CLICK_BLOCK`) whose material happened to appear in `factions.wartimeInteractableBlocks` was wrongly allowed even when that material was absent from `factions.wartimeBreakableBlocks`, because the interactable-list check ran unconditionally before the action type was considered. Which wartime permission list (breakable / interactable / placeable) applies is now decided strictly by the Bukkit action type and whether the clicked block is interactive, before any wartime list is consulted, so a block appearing in one list can no longer leak permission for a different kind of action.
- `/faction addmember` now checks whether the target faction is full before removing the target player from their current faction, so a full target faction can no longer leave the player factionless.
- `/faction gate remove` can no longer delete a gate that is in a different world from the player running the command. Gate distance was measured from coordinates alone, so a faction holding gates in both the Overworld and the Nether — where overlapping x/z coordinates are routine — could have the wrong gate silently destroyed. Distances are now world-aware, matching the behaviour already used to decide whether a position is inside a gate.
- `/faction gate remove` now respects `gates.maxRemoveDistance` at very long range. Squared distances were computed as `Int` and wrapped around beyond roughly 46,341 blocks on a single axis, turning negative, which both defeated the maximum-distance check and made a very distant gate compare as the nearest one. They are now computed as `Long`.

## [6.0.0-SNAPSHOT-7-25-2026] – 2026-07-25

### Changed
- Medieval Factions is now developed AI-first. Day-to-day feature work, grooming, review and maintenance run through AI agents working directly against this repository, with the maintainers setting direction and approving what lands. The major version bump marks that change in how the project is built — it is not a break in the plugin API, the configuration format or the database schema, and 5.x servers can upgrade in place. Released as `6.0.0-SNAPSHOT-7-25-2026`: the AI-first line has not yet been verified in live server operation, and the dated snapshot designation stays until it has.

### Added
- Configurable moderator approval for faction declarations. When enabled, `/faction declarewar`, `/faction ally`, and `/faction vassalize` create a pending request that a moderator (permission `mf.approve`, default `op`) must approve before it takes effect. Gated independently by the `factions.warDeclarationRequiresApproval`, `factions.allyDeclarationRequiresApproval`, and `factions.vassalizeDeclarationRequiresApproval` config options (all default `false`). New `/faction approve [id]`, `/faction deny [id]`, and `/faction pendingactions` commands manage requests, and a reason can be attached with `-- <reason>`.
- `/faction power` now also reports a faction's claim count. When `factions.limitLand` is enabled it is shown as `claimed/capacity` (capacity equals the faction's current power); otherwise just the number of claimed chunks is shown.
- `/faction declinevassalization [faction]` command (permission `mf.declinevassalization`, default `true`): lets a faction decline a pending vassalization request sent to it, with notifications to both factions.
- DPC community API integration: opt-in sync of faction data to `https://dansplugins.com` via `POST /api/v1/factions`. Enabled with `/mf dpc optin` and configured under the `dpc-api.*` section of `config.yml`. Requires an API key from the DPC website.
- `/mf dpc` subcommand (permission `mf.dpc`, default `op`) with `optin`, `optout`, `reminder on|off`, `shareip on|off`, `discord <link>|clear` actions.
- bStats charts for DPC opt-in rate, login-reminder usage, server-IP sharing, and Discord-link presence.

### Fixed
- Standing on a pressure plate (or tripwire, or farmland) you are not allowed to use no longer floods chat with a protection message every tick. Physical interactions are still blocked exactly as before; only the accompanying message is suppressed, for the faction-territory, wilderness and bypass notices alike.
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
