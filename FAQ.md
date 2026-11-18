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
- [Locked Areas](#locked-areas)
  - [Do locked areas replace block locks?](#do-locked-areas-replace-block-locks)
  - [Do area corners snap to chunk boundaries?](#do-area-corners-snap-to-chunk-boundaries)
  - [Can different areas let in different roles?](#can-different-areas-let-in-different-roles)
  - [Can a faction have more than one locked area?](#can-a-faction-have-more-than-one-locked-area)
  - [How do I remove a locked area I no longer need?](#how-do-i-remove-a-locked-area-i-no-longer-need)

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

---

## Additional Resources

- [Commands Reference](COMMANDS.md) - Complete list of all commands
- [User Guide](USER_GUIDE.md) - Getting started and common scenarios
- [Configuration Guide](CONFIG.md) - Server configuration options
- [Wiki FAQ](https://github.com/Dans-Plugins/Medieval-Factions/wiki/FAQ) - Community-maintained questions
