# Medieval Factions - FAQ

Answers to questions that come up often. For a full command list see [COMMANDS.md](COMMANDS.md), and for
step-by-step walkthroughs see the [User Guide](USER_GUIDE.md).

## Table of Contents
- [Disbanding Factions](#disbanding-factions)
  - [How do I disband my own faction?](#how-do-i-disband-my-own-faction)
  - [Is there a way to forcefully disband a faction?](#is-there-a-way-to-forcefully-disband-a-faction)
  - [Why does `/f disband` tell me the faction must be empty?](#why-does-f-disband-tell-me-the-faction-must-be-empty)
  - [Can I disband a faction from the console or a command block?](#can-i-disband-a-faction-from-the-console-or-a-command-block)
  - [What happens to claims and gates when a faction is disbanded?](#what-happens-to-claims-and-gates-when-a-faction-is-disbanded)
  - [Can a disband be undone?](#can-a-disband-be-undone)
- [Rivalry](#rivalry)
  - [Does a rivalry do anything?](#does-a-rivalry-do-anything)
  - [Do I need a rivalry before I can declare war?](#do-i-need-a-rivalry-before-i-can-declare-war)
  - [Who can name or end a rivalry?](#who-can-name-or-end-a-rivalry)
- [Faction Home and Respawning](#faction-home-and-respawning)
  - [Can players respawn at the faction home?](#can-players-respawn-at-the-faction-home)
  - [Can dying be the only way to reach the faction home?](#can-dying-be-the-only-way-to-reach-the-faction-home)
  - [Why do players respawn at world spawn when EssentialsX is installed?](#why-do-players-respawn-at-world-spawn-when-essentialsx-is-installed)
- [Locked Areas](#locked-areas)
  - [Do locked areas replace block locks?](#do-locked-areas-replace-block-locks)
  - [Do area corners snap to chunk boundaries?](#do-area-corners-snap-to-chunk-boundaries)
  - [Can different areas let in different roles?](#can-different-areas-let-in-different-roles)
  - [Can a faction have more than one locked area?](#can-a-faction-have-more-than-one-locked-area)
  - [How do I remove a locked area I no longer need?](#how-do-i-remove-a-locked-area-i-no-longer-need)
- [Power](#power)
  - [Can I stop a player's power from changing?](#can-i-stop-a-players-power-from-changing)
  - [Can players see their power without running a command?](#can-players-see-their-power-without-running-a-command)
- [War and Peace](#war-and-peace)
  - [Can two factions go straight back to war after making peace?](#can-two-factions-go-straight-back-to-war-after-making-peace)
  - [Can an admin start or end a war between two other factions?](#can-an-admin-start-or-end-a-war-between-two-other-factions)
- [Vassalage](#vassalage)
  - [What is `/f vassalage`?](#what-is-f-vassalage)
  - [How do I take back a vassalization offer?](#how-do-i-take-back-a-vassalization-offer)
- [Locks](#locks)
  - [Can I give a whole role access to a locked chest?](#can-i-give-a-whole-role-access-to-a-locked-chest)
  - [Can I lock armour stands and item frames?](#can-i-lock-armour-stands-and-item-frames)
- [Claims](#claims)
  - [How can I see where the claim borders are?](#how-can-i-see-where-the-claim-borders-are)
  - [How do I stop factions claiming spawn?](#how-do-i-stop-factions-claiming-spawn)
  - [Can I stop factions claiming land far from their territory?](#can-i-stop-factions-claiming-land-far-from-their-territory)
- [Roles](#roles)
  - [Can roles have a prefix in chat?](#can-roles-have-a-prefix-in-chat)
  - [How do I change the order of roles?](#how-do-i-change-the-order-of-roles)
- [Storage](#storage)
  - [Do I need a database?](#do-i-need-a-database)
- [Other Plugins](#other-plugins)
  - [Does Medieval Factions protect Slimefun machines?](#does-medieval-factions-protect-slimefun-machines)

## Disbanding Factions

### How do I disband my own faction?

Run `/f disband` with no arguments. Two conditions must be met:

1. Your role in the faction must have the `DISBAND` faction permission. Of the three default roles
   (Member, Officer, Owner), only **Owner** is granted it out of the box.
2. You must be the **only remaining member** of the faction. Everyone else has to leave or be kicked first.

You also need the `mf.disband` server permission, which is `default: true`.

### Is there a way to forcefully disband a faction?

Yes. Passing a faction name as an argument — `/f disband <faction name>` — force-disbands that faction and
**skips both of the checks above**: no `DISBAND` role permission is required, and the faction is deleted
regardless of how many members it has.

This form requires the `mf.disband.others` server permission, which is `default: op`. Grant it deliberately;
anyone who holds it can delete any faction on the server.

```
/f disband Kingdom of Example
```

Notes:

- Faction names with spaces work without quoting — everything after `disband` is treated as the name.
- The lookup is by exact faction name. Tab-completion offers matching faction names.
- The `mf.disband` permission is checked first, so revoking `mf.disband` from a player also blocks the
  forceful form for them.
- There is **no confirmation prompt**. The faction is deleted as soon as the command runs.

### Why does `/f disband` tell me the faction must be empty?

The self-disband path deliberately requires you to be the last member, so a faction cannot be deleted out
from under players who are still in it. Kick or wait for the remaining members to leave, or ask an
administrator to use the forceful form described above.

### Can I disband a faction from the console or a command block?

No. `/f disband` rejects any non-player sender, including the server console and command blocks. Forceful
disbands have to be run in-game by a player who holds `mf.disband.others`.

### What happens to claims and gates when a faction is disbanded?

Disbanding cascades: all of the faction's claimed chunks are released and all of its gates are deleted
along with the faction record. If Dynmap integration is enabled, the map is refreshed to drop the removed
territory (unless `dynmap.onlyRenderTerritoriesUponStartup` is set).

### Can a disband be undone?

No. There is no built-in undo or restore for a disbanded faction — the deletion is permanent once it
succeeds. Take a database backup before running forceful disbands on a live server.

For plugin developers: disbanding fires a cancellable `FactionDisbandEvent` before anything is deleted, so
an add-on can veto a disband (including a forceful one) if a server needs stricter rules.

## Rivalry

### Does a rivalry do anything?

A rivalry is for show. `/f rival <faction>` lists the other faction under "Rivals" in `/f info`, and both
factions are told when a rivalry is named or ended. It does not change war, PvP, territory protection, power
or any other rule.

A rivalry is one-sided: the other faction does not have to agree, and does not have to name you back. A
faction you are mutually allied with cannot be named as a rival.

### Do I need a rivalry before I can declare war?

No. `/f declarewar` works the same whether or not either faction has named the other as a rival.

### Who can name or end a rivalry?

`/f rival` and `/f unrival` need the `mf.rival` server permission (`default: true`) and the same faction role
permission as declaring war (`DECLARE_WAR`). On servers that set `factions.minimumRivalryHours` above `0`,
`/f unrival` is refused until the rivalry is that many hours old. See
[`/f rival` in COMMANDS.md](COMMANDS.md#faction-rival-faction-or-f-rival-faction).

## Faction Home and Respawning

### Can players respawn at the faction home?

Yes. Set `factions.respawnAtFactionHome: true` in `config.yml`. A player who dies then respawns at their
faction's home. Some cases are unchanged:

- A bed or respawn anchor still takes priority over the faction home.
- Only deaths count. Leaving the End does not send a player home.
- A player whose faction has no home, or whose home is in a world that is not loaded, respawns as usual.

See [`factions.respawnAtFactionHome`](CONFIG.md#factionsrespawnatfactionhome).

### Can dying be the only way to reach the faction home?

Yes. Turn on `factions.respawnAtFactionHome`, then take the `mf.home` permission away from players with
your permissions plugin (for example, negate `mf.home` in LuckPerms). The respawn does not check `mf.home`
or the faction's `GO_HOME` role permission, so players still respawn at home while `/f home` is refused.

Use the server permission rather than the `GO_HOME` role permission: role permissions belong to each
faction, so a faction owner could turn `GO_HOME` back on for their own members.

### Why do players respawn at world spawn when EssentialsX is installed?

EssentialsX Spawn also sets the respawn location. With its default `respawn-listener-priority: high`
(in `plugins/Essentials/config.yml`), it does so after Medieval Factions, so the EssentialsX spawn wins over
`factions.respawnAtFactionHome`. Set `respawn-listener-priority: none` so EssentialsX Spawn leaves respawns
alone, or `lowest` so it runs before Medieval Factions, and restart. Other EssentialsX Spawn features, such as
new-player kits and the spawn on first join, are separate settings and keep working.

When `factions.respawnAtFactionHome` is on, Medieval Factions logs a warning at startup if EssentialsX Spawn
is installed with any other value.

## Locked Areas

### Do locked areas replace block locks?

No. Block locks (`/f lock`) and locked areas (`/f area`) are separate and both work.

- A **lock** protects one block (a chest, a door) for its owner and the players or roles it lists.
- A **locked area** protects a whole box of blocks: inside it, only the players and roles you list may place,
  break or use blocks, even if they are members of your faction.

They can be combined. A player listed on a locked chest inside an area can still open that chest, even if
they are not listed on the area.

### Do area corners snap to chunk boundaries?

No. `/f area pos1` and `/f area pos2` each mark the exact block you are standing on, anywhere in your claimed
land. The area is the box between those two blocks, **including height**: it only covers the Y levels between
the two corners, so stand at floor level for one corner and at ceiling level for the other.

Every chunk the box touches must be claimed by your faction, and both corners must be in the same world.

### Can different areas let in different roles?

Yes. Each area has its own access list of players and roles:

```
/f area add vault role Officer
/f area add armoury role Guard
/f area add armoury Steve
```

`/f area remove <area> <player>` and `/f area remove <area> role <role>` take access away. Members whose role
has the `MANAGE_LOCKED_AREAS` permission are never refused inside any area. By default that is every role
that may claim land. Players with `/f bypass` on are never refused either.

### Can a faction have more than one locked area?

Yes, as many as you need. Each one needs a different name of 1 to 32 letters, digits, `-` or `_`. If areas
overlap, a player must be allowed by every area that contains the block.

### How do I remove a locked area I no longer need?

`/f area delete <name>`. `/f area list` shows every area with its corners and who has access. If the land
under an area is unclaimed or lost, the area stops protecting those chunks.

See [`/f area` in COMMANDS.md](COMMANDS.md#faction-area-or-f-area) and
[Locking Important Blocks](USER_GUIDE.md#locking-important-blocks) in the User Guide.

## Power

### Can I stop a player's power from changing?

Yes. `/power setfixed <player> <amount>` fixes a player's power at that amount. While it is fixed, nothing
changes it: not time online or offline, not kills, not deaths, and not `/power set`. `/f power` shows that the
power is fixed. This suits servers that hand out power another way, for example by rank.

`/power clearfixed <player>` removes it, and power changes normally again from the fixed amount. Both
commands need `mf.power.fixed` (default: op) or `mf.force.power`. The amount must lie between
`players.minPower` and `players.maxPower`.

See [`/power setfixed`](COMMANDS.md#power-setfixed-player-amount) in COMMANDS.md.

### Can players see their power without running a command?

Yes, with the power scoreboard. Set `powerScoreboard.enabled: true` and restart. Each online player then sees a
sidebar with their own power and, if they are in a faction, the faction's power and max power. It refreshes
every `powerScoreboard.updateIntervalSeconds` (default `5`). It is off by default, and it never replaces a
scoreboard that another plugin has already given the player.

See [Power Scoreboard](CONFIG.md#power-scoreboard) in CONFIG.md.

## War and Peace

### Can two factions go straight back to war after making peace?

Only if truces are off, which is the default. Set `factions.truceDurationMinutes` above `0` to turn them on.
When both factions have run `/f makepeace`, a truce of that many minutes starts. Until it ends:

- `/f declarewar` between the two factions is refused and says how many minutes are left,
- a moderator cannot `/f approve` a pending war declaration between them (the request is kept for later),
- `/f invoke` cannot call an ally into a war against a faction it has a truce with.

Truces are saved in `truces.yml`, so a restart does not end them.

See [`factions.truceDurationMinutes`](CONFIG.md#factionstrucedurationminutes) in CONFIG.md.

### Can an admin start or end a war between two other factions?

Yes. Operators hold `mf.force.declarewar` and `mf.force.makepeace` by default, and with them a diplomacy
command can name two factions:

```
/f declarewar FactionA FactionB
/f makepeace FactionA FactionB
```

This acts for both factions at once, with no role permission, consent or moderator approval needed. Alliances
and vassalage have the same kind of node: `mf.force.ally`, `mf.force.breakalliance`, `mf.force.vassalize` and
`mf.force.grantindependence`. The usual rules still apply: for example, allies cannot be put at war with each
other. Every forced change is written to the server log.

See [Diplomacy & Warfare](COMMANDS.md#diplomacy--warfare) in COMMANDS.md.

## Vassalage

### What is `/f vassalage`?

One command that holds all the vassalage commands:

| Subcommand | What it does | Same as |
|---|---|---|
| `/f vassalage offer <faction>` | Offers to make another faction your vassal | `/f vassalize` |
| `/f vassalage accept <faction>` | Accepts a liege's offer | `/f swearfealty` |
| `/f vassalage decline <faction>` | Declines a liege's offer | `/f declinevassalization` |
| `/f vassalage revolt` | Breaks away from your liege | `/f declareindependence` |
| `/f vassalage release <vassal>` | Frees one of your vassals | `/f grantindependence` |
| `/f vassalage rescind <faction>` | Takes back an offer that has not been accepted | (new) |

The older commands still work, with the same permissions.

### How do I take back a vassalization offer?

`/f vassalage rescind <faction>`, as long as the other faction has not accepted yet. It needs the same
permissions as sending the offer. If the offer was already accepted, use `/f vassalage release` instead.

See [Vassalage System](COMMANDS.md#vassalage-system) in COMMANDS.md.

## Locks

### Can I give a whole role access to a locked chest?

Yes. Run `/accessors list` and right-click the block, then use the **Add role** button, or type
`/accessors add <x> <y> <z> role <role>`. Every member of your faction who holds that role can then open the
block. The access follows the role: a member who is given the role gets access, and one who loses it loses
access.

Only roles of the lock owner's own faction can be added. Holding a listed role lets a player open the block,
but not change who else can.

See [`/accessors add`](COMMANDS.md#accessors-add) in COMMANDS.md.

### Can I lock armour stands and item frames?

Yes. Run `/lock` and right-click the armour stand or item frame (glow item frames too). The same rules as for
blocks apply: you must be in a faction, and it must be in your faction's land. Only you and the players on its
access list can swap its armour or take, put or rotate the item, and only you can break it. As with blocks,
explosions and mobs are not stopped.

See [Locks & Access Control](COMMANDS.md#locks--access-control) in COMMANDS.md.

## Claims

### How can I see where the claim borders are?

`/f claim show` outlines the claims within 4 chunks of you for 15 seconds: lime wool for your own faction, red
for others. Only you can see the outline, and no blocks in the world are changed.

See [`/f claim show`](COMMANDS.md#faction-claim-show-or-f-claim-show) in COMMANDS.md.

### How do I stop factions claiming spawn?

There are two ways, and both leave existing claims alone:

- List the chunks in `factions.blockedClaimChunks`, for example `world:-2:-2:1:1` for a 4x4 block of chunks.
  The numbers are chunk coordinates, not block coordinates. Listed chunks cannot be claimed or conquered.
- If spawn is already a WorldGuard region, set `worldGuard.blockClaimsInRegions: true`. New claims are then
  refused in any chunk that overlaps a WorldGuard region, apart from the regions or flags you exempt.

See [`factions.blockedClaimChunks`](CONFIG.md#factionsblockedclaimchunks) and
[WorldGuard Integration](CONFIG.md#worldguard-integration) in CONFIG.md.

### Can I stop factions claiming land far from their territory?

Yes. `factions.maxClaimDistance` sets how far, in chunks, a new claim may be from the faction's nearest claim
in the same world. A faction's first claim in a world is not limited. The default, `-1`, means no limit.

A related option, `factions.preventConqueringSurroundedChunks`, stops attackers from conquering a chunk in the
middle of enemy land: they must take the edge chunks first.

See [`factions.maxClaimDistance`](CONFIG.md#factionsmaxclaimdistance) in CONFIG.md.

## Roles

### Can roles have a prefix in chat?

Yes. `/f role setprefix <role> <prefix>` sets one, for example `/f role setprefix Knight &6[Knight]`. Leave out
the prefix to remove it. The prefix shows before the member's name in faction, ally and vassal chat, and in
public chat when `chat.enableDefaultChatFormatting` is on. Other plugins can show it through the
PlaceholderAPI placeholder `%medievalfactions_faction_role_prefix%`.

See [`/f role setprefix`](COMMANDS.md#faction-role-setprefix-role-prefix-or-f-role-setprefix-role-prefix) in
COMMANDS.md.

### How do I change the order of roles?

`/f role move <role> up` or `/f role move <role> down` moves a role one place in the list, for example to list
a new Duke role above Member. The order only changes how roles are listed; it grants and removes nothing.

## Storage

### Do I need a database?

No. Set `storage.type: json` to keep data in JSON files instead. The default is still `database`, which suits
busy servers better, because every JSON save rewrites the whole file for that kind of data.

To move existing data, run `/f migrate toJson` (or `/f migrate toDatabase`) with no players online, then stop
the server, change `storage.type` and start it again. The target must be empty: the command will not merge two
sets of data. Back up first.

See [Storage Configuration](CONFIG.md#storage-configuration) in CONFIG.md and the
[Migration Guide](docs/MIGRATION_GUIDE.md).

## Other Plugins

### Does Medieval Factions protect Slimefun machines?

Yes, if you turn it on. Set `slimefun.protection.enabled: true` and restart. In claimed land, players who
could not break or use an ordinary block there cannot use, break or place Slimefun blocks either. Wilderness
is not affected.

See [Slimefun Integration](CONFIG.md#slimefun-integration) in CONFIG.md.

---

## Additional Resources

- [Commands Reference](COMMANDS.md) - Complete list of all commands
- [User Guide](USER_GUIDE.md) - Getting started and common scenarios
- [Configuration Guide](CONFIG.md) - Server configuration options
- [Wiki FAQ](https://github.com/Dans-Plugins/Medieval-Factions/wiki/FAQ) - Community-maintained questions
