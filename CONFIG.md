# Medieval Factions - Configuration Guide

This document provides detailed information about all configuration options available in Medieval Factions.

## Table of Contents
- [General Settings](#general-settings)
- [Storage Configuration](#storage-configuration)
  - [Database Storage](#database-storage)
  - [JSON Storage](#json-storage)
  - [Migrating Between Storage Types](#migrating-between-storage-types)
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

## Storage Configuration

Medieval Factions supports two storage backends: **Database** (default) and **JSON**. You can choose which one to use based on your server's needs.

### `storage.type`
**Type:** String  
**Default:** `database`  
**Description:** Determines which storage backend to use for persisting faction data.  
**Available Values:**
- `database` - Uses a SQL database (H2, MySQL, MariaDB, PostgreSQL)
- `json` - Uses JSON files stored on disk

**When to use JSON:**
- Simpler server setups without database requirements
- Easier data inspection and manual editing (be careful!)
- Better portability for backups
- Smaller servers with fewer players

**When to use Database:**
- Larger servers with many players and factions
- Better performance for complex queries
- Concurrent access from multiple servers (with MySQL/PostgreSQL)
- Professional production environments

### Database Storage

When `storage.type` is set to `database`, the following options apply:

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

### JSON Storage

When `storage.type` is set to `json`, the following options apply:

### `storage.json.path`
**Type:** String  
**Default:** `./medieval_factions_data`  
**Description:** Directory path where JSON files will be stored. Can be relative or absolute.  
**Examples:**
- Relative path: `./medieval_factions_data`
- Absolute path: `/var/minecraft/data/medieval_factions`

**Important Notes:**
- Ensure the server has read/write permissions for this directory
- Regular backups are recommended for JSON storage
- Files are validated against JSON schemas on read/write operations
- Individual entity types are stored in separate JSON files (players.json, factions.json, etc.)

### Migrating Between Storage Types

To migrate data between storage types, you'll need to manually change the `storage.type` configuration and ensure both storage backends are properly configured. It's recommended to:

1. **Backup your data** before any migration
2. Stop your server
3. Change `storage.type` to the desired backend (`json` or `database`)
4. Configure the target storage backend appropriately
5. Start your server

The plugin will automatically use the new storage backend. Note that migration tools may be available in future versions to automate data transfer between storage types.

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
**Description:** When `true`, non-faction members can open/close doors in faction territory.

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
