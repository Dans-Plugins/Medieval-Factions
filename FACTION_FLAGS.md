# Medieval Factions - Faction Flags Reference

This document provides detailed information about faction flags and their impacts on faction behavior.

## What are Faction Flags?

Faction flags are settings that can be configured per-faction to customize how the faction operates. Unlike server-wide configuration options, flags can be set differently for each faction, allowing for diverse playstyles and strategies.

## Table of Contents
- [Managing Faction Flags](#managing-faction-flags)
- [Available Flags](#available-flags)
  - [alliesCanInteractWithLand](#alliescaninteractwithland)
  - [vassalageTreeCanInteractWithLand](#vassalagetreecaninteractwithland)
  - [liegeChainCanInteractWithLand](#liegechaincaninteractwithland)
  - [neutral](#neutral)
  - [color](#color)
  - [allowFriendlyFire](#allowfriendlyfire)
  - [acceptBonusPower](#acceptbonuspower)
  - [enableMobProtection](#enablemobprotection)
  - [protectVillagerTrade](#protectvillagertrade)
- [Default Values](#default-values)
- [Use Cases](#use-cases)

## Managing Faction Flags

### Viewing Flags
To view all available flags and their current values for your faction:
```
/faction flag list
/f flag list
```

### Setting Flags
To set a flag value for your faction:
```
/faction flag set [flag_name] [value]
/f flag set [flag_name] [value]
```

**Example:**
```
/f flag set color #FF0000
/f flag set allowFriendlyFire true
/f flag set neutral false
```

### Permissions
- **View flags:** `mf.flag.list` (default: true)
- **Set flags:** `mf.flag.set` (default: true)
- **Force set for other factions:** `mf.force.flag` (default: op)

## Available Flags

### alliesCanInteractWithLand
**Type:** Boolean (true/false)  
**Default:** `false` (configurable in config.yml)  
**Description:** Controls whether members of allied factions can build, break blocks, and interact with items in your faction's claimed territory.

**Impact:**
- When `true`: Allied faction members can build, break blocks, and use items in your territory
- When `false`: Allied faction members are treated like outsiders and cannot modify your territory

**Use Cases:**
- Set to `true` for joint building projects with allied factions
- Set to `false` to maintain strict territorial control even from allies
- Useful for defensive alliances where you don't want allies modifying your base

**Command:**
```
/f flag set alliesCanInteractWithLand true
/f flag set alliesCanInteractWithLand false
```

---

### vassalageTreeCanInteractWithLand
**Type:** Boolean (true/false)  
**Default:** `false` (configurable in config.yml)  
**Description:** Controls whether members of your vassal factions can build, break blocks, and interact with items in your claimed territory.

**Impact:**
- When `true`: Your vassals can freely build and break blocks in your territory
- When `false`: Your vassals are treated as outsiders in your territory

**Use Cases:**
- Set to `true` if you want vassals to help develop your territory
- Set to `false` for a traditional feudal hierarchy where vassals manage only their own lands
- Useful for collaborative empire-building with trusted vassals

**Command:**
```
/f flag set vassalageTreeCanInteractWithLand true
/f flag set vassalageTreeCanInteractWithLand false
```

---

### liegeChainCanInteractWithLand
**Type:** Boolean (true/false)  
**Default:** `false` (configurable in config.yml)  
**Description:** Controls whether members of your liege faction (the faction you're a vassal to) can build, break blocks, and interact with items in your claimed territory.

**Impact:**
- When `true`: Your liege's members can freely modify your territory
- When `false`: Your liege must respect your territorial boundaries

**Use Cases:**
- Set to `true` for cooperative vassal relationships where lieges help develop vassal lands
- Set to `false` to maintain autonomy while still being a vassal
- Useful for role-playing different types of feudal relationships

**Command:**
```
/f flag set liegeChainCanInteractWithLand true
/f flag set liegeChainCanInteractWithLand false
```

---

### neutral
**Type:** Boolean (true/false)  
**Default:** `false` (configurable in config.yml)  
**Description:** Declares your faction as neutral, preventing participation in wars.

**Impact:**
- When `true`: Your faction cannot declare war, be declared war upon, or participate in conflicts
- When `false`: Your faction can engage in normal diplomatic and military activities

**Requirements:**
- Server must have `factions.allowNeutrality` set to `true` in config.yml
- May have additional restrictions based on server rules

**Use Cases:**
- Merchant factions that want to trade with everyone
- Builder factions focused on construction projects
- Role-play factions representing neutral states or organizations
- Beginner factions wanting to avoid conflict

**Command:**
```
/f flag set neutral true
/f flag set neutral false
```

**Important Notes:**
- Neutral factions typically cannot have vassals or lieges
- Changing from neutral to non-neutral may have a cooldown period
- Some servers may disable this feature entirely

---

### color
**Type:** String (hex color code)  
**Default:** `random` (configurable in config.yml)  
**Description:** Sets your faction's display color used in chat, maps, and other displays.

**Format:** Hex color code in format `#RRGGBB` where:
- `RR` = Red component (00-FF)
- `GG` = Green component (00-FF)
- `BB` = Blue component (00-FF)

**Impact:**
- Affects faction name color in chat messages
- Affects faction territory color on Dynmap
- Affects faction color in UI elements and displays

**Examples:**
```
/f flag set color #FF0000  (Red)
/f flag set color #00FF00  (Green)
/f flag set color #0000FF  (Blue)
/f flag set color #FFD700  (Gold)
/f flag set color #800080  (Purple)
/f flag set color #FFA500  (Orange)
/f flag set color #FFFFFF  (White)
/f flag set color #000000  (Black)
```

**Validation:**
- Must be a valid 6-digit hexadecimal color code
- Must start with `#`
- Invalid values will be rejected

**Tips:**
- Choose colors that contrast well with common backgrounds
- Consider your faction's theme or identity when choosing a color
- Avoid colors too similar to allied factions for clarity

---

### allowFriendlyFire
**Type:** Boolean (true/false)  
**Default:** `false` (configurable in config.yml)  
**Description:** Controls whether faction members can damage each other in PVP.

**Impact:**
- When `true`: Faction members can attack and damage each other
- When `false`: Faction members are protected from damage by other members

**Use Cases:**
- Set to `true` for training purposes or friendly duels within the faction
- Set to `false` for normal operations to prevent accidental or malicious team damage
- Useful for role-play scenarios involving internal conflicts

**Command:**
```
/f flag set allowFriendlyFire true
/f flag set allowFriendlyFire false
```

**Important Notes:**
- This overrides the global `pvp.friendlyFire` config setting for your faction
- Does not affect the `/duel` command system
- May interact with other PVP-related plugins

---

### acceptBonusPower
**Type:** Boolean (true/false)  
**Default:** `true` (configurable in config.yml)  
**Description:** Controls whether your faction accepts bonus power granted by server administrators.

**Impact:**
- When `true`: Administrators can grant your faction bonus power using `/f bonuspower`
- When `false`: Your faction refuses bonus power grants, relying only on member power

**Use Cases:**
- Set to `false` for a "pure" or "no handouts" gameplay philosophy
- Set to `true` to accept event rewards or administrative compensation
- Useful for role-play factions that want to maintain independence

**Command:**
```
/f flag set acceptBonusPower true
/f flag set acceptBonusPower false
```

**Important Notes:**
- This is a faction-level decision and affects all members
- Does not affect normal power generation from members
- Existing bonus power is retained even if set to false

---

### enableMobProtection
**Type:** Boolean (true/false)  
**Default:** Value from `factions.defaults.flags.enableMobProtection` in config.yml  
**Description:** Controls whether hostile mobs are protected from damage by non-members in your faction territory.

**Impact:**
- When `true`: Non-members cannot attack or kill mobs in your territory
- When `false`: Anyone can kill mobs in your territory

**Use Cases:**
- Set to `true` to protect mob farms and grinders from outsiders
- Set to `false` for open hunting grounds
- Useful for protecting named pets or special mobs

**Command:**
```
/f flag set enableMobProtection true
/f flag set enableMobProtection false
```

**Important Notes:**
- This does not affect mob spawning rates
- May interact with the global `factions.mobsSpawnInFactionTerritory` setting
- Does not prevent mob spawning, only mob damage

---

### protectVillagerTrade
**Type:** Boolean (true/false)  
**Default:** `true` (configurable in config.yml)  
**Description:** Controls whether villager trading is restricted to faction members in your territory.

**Impact:**
- When `true`: Only faction members can trade with villagers in your territory
- When `false`: Anyone can trade with villagers in your territory

**Use Cases:**
- Set to `true` to protect your villager trading halls and custom villagers
- Set to `false` for public trading posts open to all players
- Useful for protecting valuable villager trades (e.g., mending books)

**Command:**
```
/f flag set protectVillagerTrade true
/f flag set protectVillagerTrade false
```

**Important Notes:**
- This prevents non-members from right-clicking villagers to open trade GUI
- Does not affect villager breeding or movement
- Helps prevent theft of good villager trades

---

## Default Values

Default values for faction flags are set in the server's `config.yml` file under `factions.defaults.flags`. When a new faction is created, it inherits these default values.

Server administrators can modify defaults in config.yml:
```yaml
factions:
  defaults:
    flags:
      alliesCanInteractWithLand: false
      vassalageTreeCanInteractWithLand: false
      liegeChainCanInteractWithLand: false
      neutral: false
      color: random  # or a specific hex color like '#FF0000'
      allowFriendlyFire: false
      acceptBonusPower: true
      enableMobProtection: false
      protectVillagerTrade: true
```

## Use Cases

### Trade Faction Setup
For a merchant faction that trades with everyone:
```
/f flag set neutral true
/f flag set color #FFD700
/f flag set protectVillagerTrade false
/f flag set alliesCanInteractWithLand false
```

### Military Alliance Setup
For factions in close military cooperation:
```
/f flag set alliesCanInteractWithLand true
/f flag set neutral false
/f flag set allowFriendlyFire false
/f flag set color #8B0000
```

### Peaceful Builder Faction
For a faction focused on building:
```
/f flag set neutral true
/f flag set alliesCanInteractWithLand true
/f flag set vassalageTreeCanInteractWithLand true
/f flag set enableMobProtection true
/f flag set color #00CED1
```

### Strict Hierarchical Empire
For an empire with clear liege-vassal relationships:
```
/f flag set vassalageTreeCanInteractWithLand false
/f flag set liegeChainCanInteractWithLand true
/f flag set alliesCanInteractWithLand false
/f flag set acceptBonusPower true
/f flag set color #800080
```

### Open Community Faction
For a welcoming, open faction:
```
/f flag set alliesCanInteractWithLand true
/f flag set protectVillagerTrade false
/f flag set enableMobProtection false
/f flag set neutral false
/f flag set color #32CD32
```

### Isolationist Faction
For a faction that wants minimal external interaction:
```
/f flag set neutral true
/f flag set alliesCanInteractWithLand false
/f flag set acceptBonusPower false
/f flag set protectVillagerTrade true
/f flag set enableMobProtection true
/f flag set color #2F4F4F
```

## Tips and Best Practices

1. **Review Flags Regularly:** As your faction evolves, review and adjust flags to match your current strategy.

2. **Communicate with Members:** Ensure all faction members understand what each flag does and agree with the settings.

3. **Match Your Playstyle:** Choose flag settings that align with your faction's goals and role-play identity.

4. **Test Before Committing:** If unsure about a flag, test it in a safe environment before applying it to important situations.

5. **Coordinate with Allies:** Discuss flag settings with allied factions to ensure compatible configurations.

6. **Document Your Settings:** Keep a record of your flag settings and the reasoning behind them for future reference.

7. **Consider the Meta:** Flag choices can give strategic advantages; think about how they interact with server politics.

8. **Balance Security and Cooperation:** Find the right balance between protecting your territory and enabling cooperation with trusted factions.

## Related Commands

- `/f flag list` - View all flags and their values
- `/f flag set [flag] [value]` - Set a flag value
- `/f info` - View faction information including some flag values
- `/f help flag` - Get help with flag commands

## Permissions

Standard players need these permissions to manage flags:
- `mf.flag.list` - View faction flags
- `mf.flag.set` - Modify faction flags (requires appropriate role permissions)

Administrators have additional permissions:
- `mf.force.flag` - Modify flags for any faction

## See Also

- [Commands Reference](COMMANDS.md) - Complete command documentation
- [Configuration Guide](CONFIG.md) - Server-wide configuration options
- [User Guide](USER_GUIDE.md) - Getting started and usage scenarios
