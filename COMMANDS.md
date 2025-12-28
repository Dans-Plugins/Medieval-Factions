# Medieval Factions - Commands Reference

This document provides a comprehensive list of all commands available in the Medieval Factions plugin.

## Table of Contents
- [Command Aliases](#command-aliases)
- [General Commands](#general-commands)
- [Faction Management](#faction-management)
- [Territory & Claims](#territory--claims)
- [Diplomacy & Warfare](#diplomacy--warfare)
- [Vassalage System](#vassalage-system)
- [Roles & Permissions](#roles--permissions)
- [Laws](#laws)
- [Gates](#gates)
- [Locks & Access Control](#locks--access-control)
- [Duels](#duels)
- [Power System](#power-system)
- [Chat System](#chat-system)
- [Applications](#applications)
- [Admin Commands](#admin-commands)

## Command Aliases

The main faction command can be accessed using any of the following aliases:
- `/faction`
- `/medievalfactions`
- `/mf`
- `/f`
- `/fraktion` (German)

## General Commands

### `/faction help` or `/f help`
**Permission:** `mf.help` (default: true)  
**Description:** Displays a list of helpful commands and usage information.  
**Usage:** `/f help`

### `/faction info` or `/f info`
**Permission:** `mf.info` (default: true)  
**Description:** Displays information about your faction.  
**Usage:** `/f info`

### `/faction who [player]` or `/f who [player]`
**Permission:** `mf.who` (default: true)  
**Description:** Displays which faction a player belongs to. If no player is specified, shows your own faction.  
**Usage:** 
- `/f who` - View your own faction
- `/f who PlayerName` - View another player's faction

### `/faction list` or `/f list`
**Permission:** `mf.list` (default: true)  
**Description:** Lists all factions on the server.  
**Usage:** `/f list`

### `/faction members` or `/f members`
**Permission:** `mf.members` (default: true)  
**Description:** Displays all members of a faction.  
**Usage:** `/f members`

## Faction Management

### `/faction create [name]` or `/f create [name]`
**Permission:** `mf.create` (default: true)  
**Description:** Creates a new faction with the specified name.  
**Usage:** `/f create MyFaction`  
**Notes:** Faction names are limited to 20 characters by default (configurable).

### `/faction disband` or `/f disband`
**Permission:** `mf.disband` (default: true)  
**Description:** Disbands your faction. This action is irreversible.  
**Usage:** `/f disband`  
**Notes:** You must be the faction leader to disband the faction. All claimed land will be unclaimed.

### `/faction join [faction]` or `/f join [faction]`
**Permission:** `mf.join` (default: true)  
**Description:** Joins a faction that you have been invited to.  
**Usage:** `/f join FactionName`

### `/faction leave` or `/f leave`
**Permission:** `mf.leave` (default: true)  
**Description:** Leaves your current faction.  
**Usage:** `/f leave`

### `/faction invite [player]` or `/f invite [player]`
**Permission:** `mf.invite` (default: true)  
**Description:** Invites a player to join your faction.  
**Usage:** `/f invite PlayerName`

### `/faction kick [player]` or `/f kick [player]`
**Permission:** `mf.kick` (default: true)  
**Description:** Kicks a player from your faction.  
**Usage:** `/f kick PlayerName`

### `/faction set [name|description|prefix] [value]` or `/f set [name|description|prefix] [value]`
**Permissions:** 
- `mf.rename` (default: true) - for name
- `mf.desc` (default: true) - for description
- `mf.prefix` (default: true) - for prefix

**Description:** Sets various faction properties.  
**Usage:**
- `/f set name NewName` - Changes faction name
- `/f set description "Our faction description"` - Sets faction description
- `/f set prefix [TAG]` - Sets faction prefix

### `/faction flag [list|set]` or `/f flag [list|set]`
**Permissions:** 
- `mf.flag.list` (default: true)
- `mf.flag.set` (default: true)

**Description:** Manages faction flags (settings).  
**Usage:**
- `/f flag list` - Lists all available flags and their current values
- `/f flag set [flag] [value]` - Sets a flag value

**Example:** `/f flag set color #FF0000`

See [FACTION_FLAGS.md](FACTION_FLAGS.md) for a complete list of available flags.

## Territory & Claims

### `/faction claim [radius]` or `/f claim [radius]`
**Permission:** `mf.claim` (default: true)  
**Description:** Claims the chunk you are standing in, or claims in a radius if specified.  
**Usage:**
- `/f claim` - Claims the current chunk
- `/f claim 2` - Claims chunks in a 2-chunk radius (5x5 area)

**Notes:** 
- Each chunk claimed requires power. Your faction must have enough power to claim land.
- Maximum claim radius is configurable (default: 3).

### `/faction claim auto` or `/f claim auto`
**Permission:** `mf.claim.auto` or `mf.autoclaim` (default: true)  
**Description:** Toggles automatic claiming. When enabled, chunks are automatically claimed as you walk through them.  
**Usage:** `/f claim auto`

### `/faction claim fill` or `/f claim fill`
**Permission:** `mf.claim.fill` or `mf.claimfill` (default: true)  
**Description:** Fills in unclaimed chunks that are surrounded by your faction's claims.  
**Usage:** `/f claim fill`  
**Notes:** 
- Maximum chunks that can be filled: 100 (default, configurable)
- Maximum depth of recursive filling: 50 (default, configurable)

### `/faction unclaim [radius]` or `/f unclaim [radius]`
**Permission:** `mf.unclaim` (default: true)  
**Description:** Unclaims the chunk you are standing in, or unclaims in a radius if specified.  
**Usage:**
- `/f unclaim` - Unclaims the current chunk
- `/f unclaim 2` - Unclaims chunks in a 2-chunk radius

### `/faction unclaimall` or `/f unclaimall`
**Permission:** `mf.unclaimall` (default: true)  
**Description:** Unclaims all land owned by your faction.  
**Usage:** `/f unclaimall`  
**Notes:** This action requires confirmation and cannot be undone.

### `/faction checkclaim` or `/f checkclaim`
**Permission:** `mf.checkclaim` or `mf.claim.check` (default: true)  
**Description:** Displays information about who owns the chunk you are standing in.  
**Usage:** `/f checkclaim`

### `/faction map [normal|diplomatic]` or `/f map [normal|diplomatic]`
**Permission:** `mf.map` (default: true)  
**Description:** Displays a text-based map of nearby claims.  
**Usage:**
- `/f map` - Shows normal map view
- `/f map normal` - Shows normal map view
- `/f map diplomatic` - Shows diplomatic map view with relationships

### `/faction sethome` or `/f sethome`
**Permission:** `mf.sethome` (default: true)  
**Description:** Sets your faction's home location to your current position.  
**Usage:** `/f sethome`  
**Notes:** The home must be set in your faction's claimed territory.

### `/faction home` or `/f home`
**Permission:** `mf.home` (default: true)  
**Description:** Teleports you to your faction's home location.  
**Usage:** `/f home`  
**Notes:** There is a configurable teleport delay (default: 5 seconds).

## Diplomacy & Warfare

### `/faction ally [faction]` or `/f ally [faction]`
**Permission:** `mf.ally` (default: true)  
**Description:** Sends an alliance request to another faction or accepts an incoming alliance request.  
**Usage:** `/f ally OtherFaction`

### `/faction breakalliance [faction]` or `/f breakalliance [faction]`
**Permission:** `mf.breakalliance` (default: true)  
**Description:** Breaks an existing alliance with another faction.  
**Usage:** `/f breakalliance OtherFaction`

### `/faction declarewar [faction]` or `/f declarewar [faction]`
**Permission:** `mf.declarewar` (default: true)  
**Description:** Declares war on another faction.  
**Usage:** `/f declarewar EnemyFaction`

### `/faction makepeace [faction]` or `/f makepeace [faction]`
**Permission:** `mf.makepeace` (default: true)  
**Description:** Sends a peace request to a faction you are at war with.  
**Usage:** `/f makepeace EnemyFaction`

### `/faction invoke [ally] [enemy]` or `/f invoke [ally] [enemy]`
**Permission:** `mf.invoke` (default: true)  
**Description:** Invokes an allied faction to join you in war against an enemy.  
**Usage:** `/f invoke AlliedFaction EnemyFaction`

### `/faction relationship view [faction1] [faction2]` or `/f relationship view [faction1] [faction2]`
**Permission:** `mf.relationship.view` (default: op)  
**Description:** Views the relationship between two factions.  
**Usage:** `/f relationship view Faction1 Faction2`

### `/faction relationship add [faction1] [faction2] [type]` or `/f relationship add [faction1] [faction2] [type]`
**Permission:** `mf.relationship.add` (default: op)  
**Description:** Forcefully adds a relationship between two factions (admin command).  
**Usage:** `/f relationship add Faction1 Faction2 ally`  
**Relationship Types:** `ally`, `war`, etc.

### `/faction relationship remove [faction1] [faction2] [type]` or `/f relationship remove [faction1] [faction2] [type]`
**Permission:** `mf.relationship.remove` (default: op)  
**Description:** Forcefully removes a relationship between two factions (admin command).  
**Usage:** `/f relationship remove Faction1 Faction2 ally`

## Vassalage System

### `/faction vassalize [faction]` or `/f vassalize [faction]`
**Permission:** `mf.vassalize` (default: true)  
**Description:** Sends a vassalization request to another faction, asking them to become your vassal.  
**Usage:** `/f vassalize OtherFaction`

### `/faction swearfealty [faction]` or `/f swearfealty [faction]`
**Permission:** `mf.swearfealty` (default: true)  
**Description:** Accepts a vassalization request from another faction, making them your liege.  
**Usage:** `/f swearfealty LiegeFaction`

### `/faction grantindependence [vassal]` or `/f grantindependence [vassal]`
**Permission:** `mf.grantindependence` (default: true)  
**Description:** Grants independence to one of your vassal factions.  
**Usage:** `/f grantindependence VassalFaction`

### `/faction declareindependence` or `/f declareindependence`
**Permission:** `mf.declareindependence` (default: true)  
**Description:** Declares independence from your liege faction, automatically triggering a war.  
**Usage:** `/f declareindependence`

## Roles & Permissions

### `/faction role list` or `/f role list`
**Permission:** `mf.role.list` (default: true)  
**Description:** Lists all roles in your faction.  
**Usage:** `/f role list`

### `/faction role view [role]` or `/f role view [role]`
**Permission:** `mf.role.view` (default: true)  
**Description:** Displays information about a specific role.  
**Usage:** `/f role view Officer`

### `/faction role set [player] [role]` or `/f role set [player] [role]`
**Permission:** `mf.role.set` (default: true)  
**Description:** Sets a player's role in the faction.  
**Usage:** `/f role set PlayerName Officer`

### `/faction role create [name]` or `/f role create [name]`
**Permission:** `mf.role.create` (default: true)  
**Description:** Creates a new role in your faction.  
**Usage:** `/f role create Guard`

### `/faction role delete [name]` or `/f role delete [name]`
**Permission:** `mf.role.delete` (default: true)  
**Description:** Deletes a role from your faction.  
**Usage:** `/f role delete Guard`

### `/faction role rename [name] [new name]` or `/f role rename [name] [new name]`
**Permission:** `mf.role.rename` (default: true)  
**Description:** Renames a role in your faction.  
**Usage:** `/f role rename Guard Protector`

### `/faction role setdefault [name]` or `/f role setdefault [name]`
**Permission:** `mf.role.setdefault` (default: true)  
**Description:** Sets a role as the default role for new members.  
**Usage:** `/f role setdefault Member`

### `/faction role setpermission [role] [permission] [value]` or `/f role setpermission [role] [permission] [value]`
**Permission:** `mf.role.setpermission` (default: true)  
**Description:** Sets a permission for a role.  
**Usage:** `/f role setpermission Officer claim true`

## Laws

### `/faction law add [law]` or `/f law add [law]`
**Permission:** `mf.addlaw` (default: true)  
**Description:** Adds a new law to your faction.  
**Usage:** `/f law add "No griefing within faction territory"`

### `/faction law list` or `/f law list`
**Permission:** `mf.laws` (default: true)  
**Description:** Lists all laws of your faction.  
**Usage:** `/f law list`

### `/faction law remove [id]` or `/f law remove [id]`
**Permission:** `mf.removelaw` (default: true)  
**Description:** Removes a law from your faction using its ID.  
**Usage:** `/f law remove 1`

## Gates

### `/gate create` or `/tor create`
**Permission:** `mf.gate` (default: true)  
**Description:** Initiates the gate creation process. Select blocks to form a gate structure.  
**Usage:** `/gate create`  
**Notes:**
- Minimum height: 3 blocks (default, configurable)
- Maximum blocks: 64 (default, configurable)
- Maximum gates per faction: 5 (default, configurable)

### `/gate remove` or `/tor remove`
**Permission:** `mf.gate` (default: true)  
**Description:** Removes a gate. Click on a gate block to remove the entire gate.  
**Usage:** `/gate remove`  
**Notes:** Maximum removal distance: 12 blocks (default, configurable)

### `/gate cancel` or `/tor cancel`
**Permission:** `mf.gate` (default: true)  
**Description:** Cancels the current gate creation process.  
**Usage:** `/gate cancel`

**Gate Interactions:**
- Right-click on a gate block to toggle it open/closed
- Gates can be controlled with redstone (if enabled)
- Certain blocks are restricted from gates (see config for list)

## Locks & Access Control

### `/lock [cancel]`
**Permission:** `mf.lock` (default: true)  
**Aliases:** `/verschlieBen`, `/verschliessen`, `/verrouiller`  
**Description:** Enables lock mode. Right-click on a block to lock it.  
**Usage:**
- `/lock` - Enables lock mode
- `/lock cancel` - Cancels lock mode

### `/unlock [cancel]`
**Permission:** `mf.unlock` (default: true)  
**Aliases:** `/aufschlieBen`, `/aufschliessen`, `/deverrouiller`  
**Description:** Enables unlock mode. Right-click on a locked block to unlock it.  
**Usage:**
- `/unlock` - Enables unlock mode
- `/unlock cancel` - Cancels unlock mode

### `/accessors list`
**Permission:** `mf.accessors.list` (default: true)  
**Aliases:** `/accessoren list`, `/accesseurs list`  
**Description:** Right-click on a locked block to view who can access it.  
**Usage:** `/accessors list`

### `/accessors add`
**Permission:** `mf.accessors.add` or `mf.grantaccess` (default: true)  
**Aliases:** `/accessoren add`, `/accesseurs add`  
**Description:** Right-click on a locked block, then click on a player to grant them access.  
**Usage:** `/accessors add`

### `/accessors remove`
**Permission:** `mf.accessors.remove` or `mf.revokeaccess` (default: true)  
**Aliases:** `/accessoren remove`, `/accesseurs remove`  
**Description:** Right-click on a locked block, then click on a player to revoke their access.  
**Usage:** `/accessors remove`

## Duels

### `/duel challenge [player]` or `/duell challenge [player]`
**Permission:** `mf.duel` (default: true)  
**Description:** Challenges another player to a duel.  
**Usage:** `/duel challenge PlayerName`  
**Notes:** Duel duration is configurable (default: 2 minutes)

### `/duel accept [player]` or `/duell accept [player]`
**Permission:** `mf.duel` (default: true)  
**Description:** Accepts a duel challenge from another player.  
**Usage:** `/duel accept PlayerName`

### `/duel cancel [player]` or `/duell cancel [player]`
**Permission:** `mf.duel` (default: true)  
**Description:** Cancels or declines a duel challenge.  
**Usage:** `/duel cancel PlayerName`

## Power System

### `/faction power` or `/f power`
**Permission:** `mf.power` (default: true)  
**Description:** Displays power statistics for yourself or your faction.  
**Usage:** `/f power`

### `/power set [player] [amount]`
**Permission:** `mf.power.set` or `mf.force.power` (default: op)  
**Aliases:** `/macht set`, `/pouvoir set`  
**Description:** Sets a player's power level (admin command).  
**Usage:** `/power set PlayerName 15`

### `/faction bonuspower [faction] [amount]` or `/f bonuspower [faction] [amount]`
**Permission:** `mf.force.bonuspower` (default: op)  
**Description:** Sets bonus power for a faction (admin command).  
**Usage:** `/f bonuspower FactionName 10`

## Chat System

### `/faction chat [faction|vassals|allies]` or `/f chat [faction|vassals|allies]`
**Permission:** `mf.chat` (default: true)  
**Description:** Toggles faction chat modes.  
**Usage:**
- `/f chat faction` - Toggles faction-only chat
- `/f chat vassals` - Toggles chat with vassals
- `/f chat allies` - Toggles chat with allies

### `/faction chat history` or `/f chat history`
**Permission:** `mf.chat.history` (default: true)  
**Description:** Views chat history.  
**Usage:** `/f chat history`

## Applications

### `/apply [faction]`
**Permission:** `mf.apply` (default: true)  
**Description:** Sends an application to join a faction.  
**Usage:** `/apply FactionName`

### `/showapps`
**Permission:** `mf.showapps` (default: true)  
**Description:** Shows pending applications to your faction.  
**Usage:** `/showapps`

### `/approveapp [player]`
**Permission:** `mf.approveapp` (default: true)  
**Description:** Approves a player's application to join your faction.  
**Usage:** `/approveapp PlayerName`

### `/denyapp [player]`
**Permission:** `mf.denyapp` (default: true)  
**Description:** Denies a player's application to join your faction.  
**Usage:** `/denyapp PlayerName`

## Admin Commands

### `/faction bypass` or `/f bypass`
**Permission:** `mf.bypass` (default: op)  
**Description:** Toggles bypass mode, allowing you to bypass faction protections.  
**Usage:** `/f bypass`

### `/faction admin create [name]` or `/f admin create [name]`
**Permission:** `mf.admin.create` (default: op)  
**Description:** Creates a leaderless faction that can exist without members.  
**Usage:** `/f admin create FactionName`  
**Notes:** Requires `allowLeaderlessFactions` to be enabled in config.

### `/faction admin setleader [faction] [player]` or `/f admin setleader [faction] [player]`
**Permission:** `mf.admin.setleader` (default: op)  
**Description:** Sets a player as the leader of a faction.  
**Usage:** `/f admin setleader FactionName PlayerName`

### `/faction addmember [faction] [player]` or `/f addmember [faction] [player]`
**Permission:** `mf.force.addmember` (default: op)  
**Description:** Forcefully adds a player to a faction (admin command).  
**Usage:** `/f addmember FactionName PlayerName`

---

## Permission Groups

### Player Permissions
All permissions with `default: true` are available to regular players.

### Admin Permissions
The `mf.admin` permission grants access to all admin commands including:
- `mf.force.*` - All force commands
- `mf.bypass` - Bypass protections
- `mf.relationship.*` - Manage relationships
- `mf.power.set` - Set player power
- `mf.admin.create` - Create leaderless factions
- `mf.admin.setleader` - Set faction leaders

## Notes

- Commands in `[brackets]` are required parameters
- Commands in `(parentheses)` are optional parameters
- Many commands have shortened aliases (e.g., `/f` instead of `/faction`)
- Admin commands require operator status by default
- Power requirements apply to land claims and faction size
- Some features may be disabled in the configuration
