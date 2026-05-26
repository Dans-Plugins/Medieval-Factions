# Medieval Factions - Configuration Guide

This document provides detailed information about all configuration options available in Medieval Factions.

## Table of Contents
- [General Settings](#general-settings)
- [Database Configuration](#database-configuration)
- [Player Power System](#player-power-system)
- [Wilderness Settings](#wilderness-settings)
- [PVP Settings](#pvp-settings)
- [Faction Settings](#faction-settings)
- [Faction Default Flags](#faction-default-flags)
- [Chat Settings](#chat-settings)
- [Duels](#duels)
- [Dynmap Integration](#dynmap-integration)
- [Gates](#gates)
- [Developer Options](#developer-options)

## General Settings

### `version`
**Type:** String  
**Default:** `@version@` (auto-generated)  
**Description:** Plugin version number. This is automatically set during build and should not be modified manually.

### `language`
**Type:** String  
**Default:** `en-US`  
**Description:** Sets the default language for the plugin. Available languages depend on installed language files.  
**Available Values:** `en-US`, `es-ES`, `ru-RU`, `pt-BR`, `de-DE`, `nl-NL`, etc.

## Database Configuration

Medieval Factions uses a database to store faction data, claims, and player information.

### `database.url`
**Type:** String  
**Default:** `jdbc:h2:./medieval_factions_db;AUTO_SERVER=true;MODE=MYSQL;DATABASE_TO_UPPER=false`  
**Description:** JDBC connection URL for the database.  
**Examples:**
- H2 (default): `jdbc:h2:./medieval_factions_db;AUTO_SERVER=true;MODE=MYSQL;DATABASE_TO_UPPER=false`
- MySQL: `jdbc:mysql://localhost:3306/medievalfactions`
- PostgreSQL: `jdbc:postgresql://localhost:5432/medievalfactions`

### `database.dialect`
**Type:** String  
**Default:** `H2`  
**Description:** Database dialect to use. Must match your database type.  
**Available Values:** `H2`, `MySQL`, `PostgreSQL`

### `database.username`
**Type:** String  
**Default:** `sa`  
**Description:** Database username for authentication.

### `database.password`
**Type:** String  
**Default:** `` (empty)  
**Description:** Database password for authentication.  
**Security Note:** Consider using environment variables or secure storage for production passwords.

## Player Power System

The power system determines how much land a faction can claim and player impact in the world.

### `players.initialPower`
**Type:** Integer  
**Default:** `5`  
**Description:** The amount of power a player starts with when joining the server.  
**Range:** Any positive integer

### `players.maxPower`
**Type:** Integer  
**Default:** `20`  
**Description:** The maximum power a player can accumulate.  
**Range:** Any positive integer

### `players.minPower`
**Type:** Integer  
**Default:** `-5`  
**Description:** The minimum power a player can have.  
**Range:** Any negative integer or 0

### `players.hoursToReachMaxPower`
**Type:** Integer  
**Default:** `12`  
**Description:** Number of hours of playtime required to reach maximum power from initial power.  
**Range:** Any positive integer

### `players.hoursToReachMinPower`
**Type:** Integer  
**Default:** `72`  
**Description:** Number of hours required to reach minimum power from initial power through deaths.  
**Range:** Any positive integer

### `players.powerLostOnDeath`
**Type:** Integer  
**Default:** `1`  
**Description:** Amount of power a player loses when they die.  
**Range:** Any positive integer or 0

### `players.powerGainedOnKill`
**Type:** Integer  
**Default:** `1`  
**Description:** Amount of power a player gains when they kill another player.  
**Range:** Any positive integer or 0

## Wilderness Settings

Controls behavior and alerts in unclaimed wilderness areas.

### `wilderness.color`
**Type:** String (hex color)  
**Default:** `#3e8e39`  
**Description:** The color used to represent wilderness on maps and in displays.  
**Format:** Hex color code (e.g., `#3e8e39`)

### `wilderness.interaction.prevent`
**Type:** Boolean  
**Default:** `false`  
**Description:** When `true`, prevents block interactions (buttons, levers, etc.) in wilderness.

### `wilderness.interaction.alert`
**Type:** Boolean  
**Default:** `false`  
**Description:** When `true`, sends an alert message when players interact with blocks in wilderness.

### `wilderness.place.prevent`
**Type:** Boolean  
**Default:** `false`  
**Description:** When `true`, prevents players from placing blocks in wilderness.

### `wilderness.place.alert`
**Type:** Boolean  
**Default:** `true`  
**Description:** When `true`, sends an alert when players place blocks in wilderness.

### `wilderness.break.prevent`
**Type:** Boolean  
**Default:** `false`  
**Description:** When `true`, prevents players from breaking blocks in wilderness.

### `wilderness.break.alert`
**Type:** Boolean  
**Default:** `true`  
**Description:** When `true`, sends an alert when players break blocks in wilderness.

## PVP Settings

Controls player vs player combat mechanics.

### `pvp.enabledForFactionlessPlayers`
**Type:** Boolean  
**Default:** `true`  
**Description:** When `true`, players without a faction can engage in PVP.

### `pvp.warRequiredForPlayersOfDifferentFactions`
**Type:** Boolean  
**Default:** `true`  
**Description:** When `true`, factions must be at war before their members can damage each other.

### `pvp.friendlyFire`
**Type:** Boolean  
**Default:** `false`  
**Description:** When `true`, faction members can damage each other (unless overridden by faction flag).

### `pvp.grantPowerToKillerIfVictimHasZeroPower`
**Type:** Boolean  
**Default:** `false`  
**Description:** When `true`, killing a player with zero or negative power still grants power to the killer.

### `pvp.enableWartimeBlockDestruction`
**Type:** Boolean  
**Default:** `false`  
**Description:** When `true`, allows players to destroy blocks in enemy faction territory during war.

## Faction Settings

### `factions.mobsSpawnInFactionTerritory`
**Type:** Boolean  
**Default:** `false`  
**Description:** When `false`, prevents hostile mob spawning in faction territory (except for spawn reasons in allowedMobSpawnReasons).

### `factions.allowedMobSpawnReasons`
**Type:** List of Strings  
**Default:** See config.yml for full list  
**Description:** List of spawn reasons that are allowed even when `mobsSpawnInFactionTerritory` is false.  
**Common Values:**
- `BREEDING` - Allow breeding
- `SPAWNER` - Allow spawners
- `SPAWNER_EGG` - Allow spawn eggs
- `CUSTOM` - Allow custom spawns
- `NATURAL` - Allow natural spawns (not in default list)

### `factions.laddersPlaceableInEnemyFactionTerritory`
**Type:** Boolean  
**Default:** `true`  
**Description:** When `true`, allows placing ladders in enemy faction territory (useful for sieges).

### `factions.wartimePlaceableBlocks`
**Type:** List of Strings  
**Default:** `[]` (empty)  
**Description:** List of block types (by Material name) that become placeable in enemy faction territory during war. Blocks listed here can be placed by an attacker whose faction is at war with the territory owner.  
**Example:**
```yaml
factions:
  wartimePlaceableBlocks:
    - SCAFFOLDING
    - TORCH
```

### `factions.wartimeBreakableBlocks`
**Type:** List of Strings  
**Default:** `[]` (empty)  
**Description:** List of block types (by Material name) that become breakable in enemy faction territory during war. Blocks listed here can be broken by an attacker whose faction is at war with the territory owner.  
**Example:**
```yaml
factions:
  wartimeBreakableBlocks:
    - SCAFFOLDING
```

### `factions.wartimeInteractableBlocks`
**Type:** List of Strings  
**Default:** `[]` (empty)  
**Description:** List of block types (by Material name) that become interactable in enemy faction territory during war. Blocks listed here can be interacted with (e.g., opened, pressed) by an attacker whose faction is at war with the territory owner.  
**Example:**
```yaml
factions:
  wartimeInteractableBlocks:
    - OAK_DOOR
    - IRON_DOOR
    - OAK_FENCE_GATE
    - STONE_BUTTON
```

### `factions.maxNameLength`
**Type:** Integer  
**Default:** `20`  
**Description:** Maximum length for faction names.  
**Range:** 1-255

### `factions.zeroPowerFactionsGetDisbanded`
**Type:** Boolean  
**Default:** `false`  
**Description:** When `true`, factions with zero or negative total power are automatically disbanded.

### `factions.vassalPowerContributionMultiplier`
**Type:** Float  
**Default:** `0.75`  
**Description:** Multiplier for how much power vassals contribute to their liege.  
**Example:** With 0.75, a vassal with 100 power contributes 75 power to their liege.  
**Range:** 0.0 to 1.0

### `factions.nonMembersCanInteractWithDoors`
**Type:** Boolean  
**Default:** `false`  
**Description:** When `true`, non-faction members can open/close doors (including trapdoors and fence gates) in faction territory.

### `factions.maxClaimRadius`
**Type:** Integer  
**Default:** `3`  
**Description:** Maximum radius (in chunks) that can be claimed at once using `/f claim [radius]`.  
**Example:** A radius of 3 allows claiming a 7x7 chunk area (49 chunks total).

### `factions.limitLand`
**Type:** Boolean  
**Default:** `true`  
**Description:** When `true`, limits the amount of land a faction can claim based on their total power.

### `factions.contiguousClaims`
**Type:** Boolean  
**Default:** `false`  
**Description:** When `true`, requires all faction claims to be connected (adjacent) to existing claims.

### `factions.blockedClaimWorlds`
**Type:** List of Strings  
**Default:** `[]` (empty list)  
**Description:** List of world names where claiming is not allowed.  
**Example:** `["world_nether", "world_the_end"]`

### `factions.actionBarTerritoryIndicator`
**Type:** Boolean  
**Default:** `true`  
**Description:** When `true`, shows territory information in the action bar when entering new chunks.

### `factions.titleTerritoryIndicator`
**Type:** Boolean  
**Default:** `true`  
**Description:** When `true`, shows territory information as a title when entering new chunks.

### `factions.titleTerritoryFadeInLength`
**Type:** Integer  
**Default:** `5`  
**Description:** Duration (in ticks) for title fade-in animation.  
**Note:** 20 ticks = 1 second

### `factions.titleTerritoryDuration`
**Type:** Integer  
**Default:** `20`  
**Description:** Duration (in ticks) that the title remains on screen.  
**Note:** 20 ticks = 1 second

### `factions.titleTerritoryFadeOutLength`
**Type:** Integer  
**Default:** `5`  
**Description:** Duration (in ticks) for title fade-out animation.  
**Note:** 20 ticks = 1 second

### `factions.allowNeutrality`
**Type:** Boolean  
**Default:** `false`  
**Description:** When `true`, allows factions to declare themselves as neutral, preventing them from participating in wars.

### `factions.factionlessFactionName`
**Type:** String  
**Default:** `Factionless`  
**Description:** Display name for players who are not in any faction.

### `factions.factionHomeTeleportDelay`
**Type:** Integer  
**Default:** `5`  
**Description:** Delay (in seconds) before teleporting to faction home.  
**Note:** Movement cancels the teleport.

### `factions.maxMembers`
**Type:** Integer  
**Default:** `-1`  
**Description:** Maximum number of members a faction can have.  
**Special Values:** `-1` = unlimited

### `factions.claimFillMaxChunks`
**Type:** Integer  
**Default:** `100`  
**Description:** Maximum number of chunks that can be filled with a single `/f claim fill` command.

### `factions.claimFillMaxDepth`
**Type:** Integer  
**Default:** `50`  
**Description:** Maximum recursion depth for the claim fill algorithm (prevents infinite loops).

### `factions.allowLeaderlessFactions`
**Type:** Boolean  
**Default:** `false`  
**Description:** When `true`, factions can exist without any members. When the last member leaves, the faction and its claims remain intact. Operators can create leaderless factions using `/faction admin create`.

## Faction Default Flags

These settings define the default values for faction flags when a new faction is created. Individual factions can override these with `/f flag set`.

### `factions.defaults.flags.alliesCanInteractWithLand`
**Type:** Boolean  
**Default:** `false`  
**Description:** Default setting for whether allied factions can interact with (build/break) in faction territory.

### `factions.defaults.flags.vassalageTreeCanInteractWithLand`
**Type:** Boolean  
**Default:** `false`  
**Description:** Default setting for whether vassals can interact with their liege's land.

### `factions.defaults.flags.liegeChainCanInteractWithLand`
**Type:** Boolean  
**Default:** `false`  
**Description:** Default setting for whether lieges can interact with their vassal's land.

### `factions.defaults.flags.neutral`
**Type:** Boolean  
**Default:** `false`  
**Description:** Default neutrality status for new factions.  
**Note:** Requires `factions.allowNeutrality` to be `true`.

### `factions.defaults.flags.color`
**Type:** String  
**Default:** `random`  
**Description:** Default color for new factions.  
**Values:** 
- `random` - Assigns a random color
- Hex color code (e.g., `#FF0000`)

### `factions.defaults.flags.allowFriendlyFire`
**Type:** Boolean  
**Default:** `false`  
**Description:** Default setting for whether faction members can damage each other.

### `factions.defaults.flags.acceptBonusPower`
**Type:** Boolean  
**Default:** `true`  
**Description:** Default setting for whether the faction accepts bonus power from admins.

### `factions.defaults.flags.protectVillagerTrade`
**Type:** Boolean  
**Default:** `true`  
**Description:** Default setting for whether villager trading is protected in faction territory (non-members cannot trade).

### `factions.defaults.flags.enableMobProtection`
**Type:** Boolean  
**Default:** Not currently set in config.yml (uses hardcoded default of `false`)  
**Description:** Default setting for whether hostile mobs are protected from damage by non-members in faction territory. While the flag is available for factions to set, this config option is not currently implemented in the default config file.  
**Note:** Factions can still set this flag individually using `/f flag set enableMobProtection [true/false]`.

## Chat Settings

### `chat.enableDefaultChatFormatting`
**Type:** Boolean  
**Default:** `true`  
**Description:** When `true`, enables Medieval Factions custom chat formatting.

### `chat.faction.format`
**Type:** String  
**Default:** `&7[faction] [${factionColor}${faction}&7] [${role}] &f${displayName}: ${message}`  
**Description:** Format for faction-only chat messages.  
**Available Variables:**
- `${factionColor}` - Faction's color
- `${faction}` - Faction name
- `${role}` - Player's role
- `${displayName}` - Player's display name
- `${message}` - The chat message

### `chat.vassals.format`
**Type:** String  
**Default:** `&7[vassals] [${factionColor}${faction}&7] [${role}] &f${displayName}: ${message}`  
**Description:** Format for vassal chat messages (chat between liege and vassals).

### `chat.allies.format`
**Type:** String  
**Default:** `&7[allies] [${factionColor}${faction}&7] [${role}] &f${displayName}: ${message}`  
**Description:** Format for ally chat messages (chat between allied factions).

## Duels

### `duels.duration`
**Type:** Duration (ISO-8601)  
**Default:** `PT2M`  
**Description:** Duration of duels before they are automatically concluded.  
**Format:** ISO-8601 duration format
- `PT2M` = 2 minutes
- `PT5M` = 5 minutes
- `PT1H` = 1 hour

### `duels.notificationDistance`
**Type:** Integer  
**Default:** `64`  
**Description:** Distance (in blocks) within which players are notified of nearby duels.

## Dynmap Integration

Dynmap integration allows faction claims to be displayed on a dynamic web map.

### `dynmap.enableDynmapIntegration`
**Type:** Boolean  
**Default:** `true`  
**Description:** Enables or disables Dynmap integration.  
**Note:** Requires Dynmap plugin to be installed.

### `dynmap.showRealms`
**Type:** Boolean  
**Default:** `false`  
**Description:** Shows realm information on the map.

### `dynmap.showDescription`
**Type:** Boolean  
**Default:** `true`  
**Description:** Shows faction descriptions in map popups.

### `dynmap.showMembers`
**Type:** Boolean  
**Default:** `false`  
**Description:** Shows faction member count in map popups.

### `dynmap.showLiege`
**Type:** Boolean  
**Default:** `false`  
**Description:** Shows faction's liege in map popups.

### `dynmap.showVassals`
**Type:** Boolean  
**Default:** `false`  
**Description:** Shows faction's vassals in map popups.

### `dynmap.showAllies`
**Type:** Boolean  
**Default:** `false`  
**Description:** Shows faction's allies in map popups.

### `dynmap.showAtWarWith`
**Type:** Boolean  
**Default:** `false`  
**Description:** Shows factions that this faction is at war with in map popups.

### `dynmap.showPower`
**Type:** Boolean  
**Default:** `false`  
**Description:** Shows faction power in map popups.

### `dynmap.showDemesne`
**Type:** Boolean  
**Default:** `false`  
**Description:** Shows demesne information in map popups.

### `dynmap.onlyRenderTerritoriesUponStartup`
**Type:** Boolean  
**Default:** `false`  
**Description:** When `true`, only renders territories once at startup instead of updating dynamically.

### `dynmap.debug`
**Type:** Boolean  
**Default:** `false`  
**Description:** Enables debug logging for Dynmap integration.

### `dynmap.fillClaims`
**Type:** Boolean  
**Default:** `true`  
**Description:** Whether to fill faction territories with color.  
**Values:**
- `true` - Fill entire territory with color (more visible)
- `false` - Only show border lines (better performance)

### `dynmap.fillOpacity`
**Type:** Float  
**Default:** `0.35`  
**Description:** Controls transparency of territory fill color.  
**Range:** 0.0 (invisible) to 1.0 (solid)

## Gates

Gates are structures that can be toggled open and closed by faction members.

### `gates.minHeight`
**Type:** Integer  
**Default:** `3`  
**Description:** Minimum height (in blocks) for a valid gate.

### `gates.maxBlocks`
**Type:** Integer  
**Default:** `64`  
**Description:** Maximum number of blocks that can make up a single gate.

### `gates.maxPerFaction`
**Type:** Integer  
**Default:** `5`  
**Description:** Maximum number of gates a single faction can create.

### `gates.maxRemoveDistance`
**Type:** Integer  
**Default:** `12`  
**Description:** Maximum distance (in blocks) from which a gate can be removed.

### `gates.restrictedBlocks`
**Type:** List of Strings  
**Default:** See config.yml for full list  
**Description:** List of block types that cannot be used in gates (typically gravity-affected or fragile blocks).  
**Examples:** `SAND`, `GRAVEL`, `TORCH`, `LADDER`, etc.

## Developer Options

### `dev.enableDevCommands`
**Type:** Boolean  
**Default:** `false`  
**Description:** Enables developer commands for testing and debugging.  
**Note:** Should only be enabled in development environments.

---

## DPC Community API

Settings for the DPC (Dans Plugins Community) API integration.
When enabled, the plugin periodically shares faction data with the DPC community website ([dansplugins.com](https://dansplugins.com)), allowing players and server owners to browse factions across the community.
This integration is **strictly opt-in**.

### `dpc-api.enabled`
**Type:** Boolean  
**Default:** `false`  
**Description:** Whether this server is opted in to sharing faction data with the DPC community. When `true`, the plugin will periodically push faction data to the DPC API.

### `dpc-api.url`
**Type:** String  
**Default:** `"https://dansplugins.com/api/v1/factions"`  
**Description:** The full endpoint URL of the DPC API. Include a port if the API runs on a non-standard port (e.g. `"https://dansplugins.com:8080/api/v1/factions"`).

### `dpc-api.key`
**Type:** String  
**Default:** `""`  
**Description:** The API key used to authenticate with the DPC API. To obtain a key, visit [dansplugins.com](https://dansplugins.com), create an account or sign in, then generate a key from your account page. Required for faction data to be submitted successfully.

### `dpc-api.server-id`
**Type:** String  
**Default:** `""`  
**Description:** A unique identifier for this server (e.g. `"my-survival-server"`). Used to identify your server in the DPC registry. Must be set before faction data can be synced. Allowed characters are letters, digits, dot, underscore, colon, and hyphen (`[A-Za-z0-9._:-]`); whitespace and other punctuation are rejected to prevent accidental near-duplicate registry partitions. Once you have started syncing under a given `server-id`, treat it as permanent — changing it strands all factions previously registered under the old id (they remain visible until manually removed).

### `dpc-api.login-reminder`
**Type:** Boolean  
**Default:** `true`  
**Description:** Whether to show a chat reminder to operators on login when the DPC API is not yet enabled. Set to `false` to suppress the reminder.

### `dpc-api.share-server-ip`
**Type:** Boolean  
**Default:** `false`  
**Description:** Whether to include the server IP address in the faction data sent to DPC. Enabling this allows your server to be advertised on the DPC website alongside your factions.

### `dpc-api.server-address`
**Type:** String  
**Default:** `""`  
**Description:** An explicit server address to include in the DPC API payload when `share-server-ip` is `true` (e.g. `"play.myserver.com:25565"`). If blank, the plugin auto-detects from the server binding (ip:port). Set this when your server is behind a proxy or when the bound IP doesn't match your public address.

### `dpc-api.discord-link`
**Type:** String  
**Default:** `""`  
**Description:** An optional Discord invite link to display on the DPC website alongside your faction data. Leave empty to omit. The link must start with `https://discord.gg/` or `https://discord.com/`; other formats are rejected at command time (and silently omitted from the sync payload if set directly in config.yml). Use `/mf dpc discord <link>` or `/mf dpc discord clear` to manage this value at runtime.

### `dpc-api.sync-interval-minutes`
**Type:** Integer  
**Default:** `10`  
**Description:** How often (in minutes) the plugin syncs faction data to the DPC API. Minimum value is 1 minute. Lower values mean more frequent updates but increase network traffic.

### DPC API troubleshooting

If a sync returns a non-2xx status, the plugin logs a `WARNING` with the
status code and a truncated response body to your server log. Common cases:

- **`401 Unauthorized`** — `dpc-api.key` is missing or wrong. Re-generate the
  key from your DPC account page (`https://dansplugins.com/account`).
- **`400 Bad Request`** — usually means `dpc-api.server-id` contains
  disallowed characters. Allowed characters are letters, digits, dot,
  underscore, colon, and hyphen.
- **`429` / `5xx`** — transient server-side issue; the next sync will retry.
  Failed requests never crash the plugin.

The DPC API server keeps a `last_synced_at` timestamp per faction and applies
several safety guards before marking missing-from-batch factions as
disbanded. If you see fewer disbands on the registry than expected (e.g. you
disbanded one of two factions and the other still appears active for a
cycle), that's the ratio guard at work — it's intentional and self-corrects
within one or two sync cycles. See the
[dpc-api README](https://github.com/Dans-Plugins/dansplugins-dot-com/blob/main/dpc-api/README.md#sync-safety-guards)
for the full server-side semantics.

---

## Configuration Best Practices

1. **Backup First:** Always backup your config before making changes.
2. **Test Changes:** Test configuration changes on a development server before applying to production.
3. **Restart Required:** Most configuration changes require a server restart to take effect.
4. **YAML Syntax:** Ensure proper YAML syntax (indentation, quotes, etc.).
5. **Performance Impact:** Some settings (like Dynmap integration) may impact server performance.
6. **Balance:** Carefully balance power settings to ensure fair gameplay.

## Common Configuration Scenarios

### Peaceful Server
For a more peaceful, community-focused server:
```yaml
pvp.warRequiredForPlayersOfDifferentFactions: true
pvp.friendlyFire: false
pvp.enableWartimeBlockDestruction: false
factions.allowNeutrality: true
```

### Hardcore PVP Server
For intense PVP gameplay:
```yaml
pvp.warRequiredForPlayersOfDifferentFactions: false
pvp.enableWartimeBlockDestruction: true
players.powerLostOnDeath: 2
factions.zeroPowerFactionsGetDisbanded: true
```

### Performance Optimization
To improve server performance:
```yaml
dynmap.fillClaims: false
dynmap.onlyRenderTerritoriesUponStartup: true
factions.actionBarTerritoryIndicator: false
factions.titleTerritoryIndicator: true
```

### Realistic Medieval Setting
For a more realistic medieval experience:
```yaml
factions.laddersPlaceableInEnemyFactionTerritory: true
factions.vassalPowerContributionMultiplier: 0.75
pvp.warRequiredForPlayersOfDifferentFactions: true
factions.allowNeutrality: true
duels.duration: PT5M
```
