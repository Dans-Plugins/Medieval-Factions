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

---

## Additional Resources

- [Commands Reference](COMMANDS.md) - Complete list of all commands
- [User Guide](USER_GUIDE.md) - Getting started and common scenarios
- [Configuration Guide](CONFIG.md) - Server configuration options
- [Wiki FAQ](https://github.com/Dans-Plugins/Medieval-Factions/wiki/FAQ) - Community-maintained questions
