## List of Commands

### **Nation Creation** ###

A new beginning- rise or fall, that's all up to you.

Use `/mf create` to establish your new faction.

**Nation Recruitment**
> Power comes in number
>
> To recruit others, you may invite them using: `/mf invite [player-name]`
> Use `/mf join [faction-name]` to prompt the leader of a faction with an approval request or accept an invitation.

**Management**
> To rename your faction, use the command: `/mf rename [new-name]`
>
> To set your faction description, use the command: `/mf desc [description]`
>
> To promote a player to an officer of your faction, use the command: `/mf promote [player-name]`
>
> To set your faction's main base, use the command: `/mf sethome`
>
> To teleport to your faction's main-base use the command: `/mf home`

**Nation Laws**
> Order is important to uphold the integrity of a faction and protect it from chaos.
>
> To view the laws of your faction, use the command: `/mf laws`
>
> To add a law, use the command: `/mf al [law description]`
>
> To edit a law, use the command: `/mf el [law number]`
>
> To remove a law, use the command: `/mf rl [law number] [new law description]`

**Nation Territory**
> Territory is an essential part of any faction, therefore it's crucial to have just enough to sustain your people.
>
> In order to claim land, you must have the appropriate amount of power.
>
> To check the status [whether it is claimed or not] of the current land you're in, use the command: `/mf cc`
>
> To claim land for your faction, use the command: `/mf claim`
>
> To remove your claim on a specific piece of land from your faction, use the command: `/mf unclaim`
>
> To enable auto-claiming [claims land that you walk on automatically], use the command: `/mf ac`
>
> To remove your claim from all your faction's land, use the command: `/mf ua`
>
> To view a text-based map of your claims, use the command: `/mf map`

**Warfare**
> Without war, one cannot achieve prosperity nor glory.
>
> To view a list of faction/s that you are currently at war with, use the command: `/mf info`
>
> To declare war on a faction, use the command: `/mf dw [faction-name]`
>
> To offer white peace to a faction, use the command: `/mf mp`
>
> To conquer someone's land, use the command: `/mf claim` in their territory.
> P.S. [This is only possible if you are at war with the targeted faction and if they have more land than power.]

**Alliances**
> Form strong bonds with other leaders and rise together under a united banner.
>
> To view a list of faction/s that you are allied with, use the command: `/mf info`
>
> To request the formation of an alliance with another faction, use the command: `/mf ally [faction-name]`
>
> To break your ties with another faction, use the command: `/mf breakalliance [faction-name]`
>
> P.S. [Allies can't declare war on each other | Both factions must send an alliance request in order for the alliance to be formed.]

Addons [Optional]

**Vassals**
> The integration and establishment of subordinating factions are critical in growth.

> To swear your loyalty to another faction or accept an offer, use the command: `/mf swearfealty`
>
> To make an offer to vassalize a faction, use the command: `/mf vassalize [faction-name]`

**Fiefs**
>Fiefs are sub-factions or divisions of a faction that members can create.

> To create a fief, use the command: `/fi create [fief-name]`
>
> To invite players into your fief, use the command: `/fi invite [player-name]`
>
> To disband your fief, use the command: `/fi disband`

### Basic Commands: ###

| Command  | Permission | Usage | Description |
| ------------- | ------------- | ------------- | ------------- |
| /mf addlaw | mf.addlaw | /mf addlaw "" | Add a law to your faction. |
| /mf editlaw | mf.editlaw |  | Edit a law in your faction. |
| /mf laws | mf.laws | /mf laws | List your faction's laws. |
| /mf addally | mf.addally |  | Attempt to ally with a faction. |
| /mf removelaw | mf.removelaw | - | Remove a law from your faction. |
| /mf autoclaim | mf.autoclaim |  | Attempt to ally with a faction. |
| /mf breakalliance | mf.breakalliance |  | Break an alliance with a faction. |
| /mf chat | mf.chat |  | Toggles faction chat. |
| /mf checkaccess | mf.checkaccess |  | Check who has access to a locked block. |
| /mf checkclaim | mf.checkclaim |  | Check if land is claimed. |
| /mf claim | mf.claim |  | Claim land for your faction. |
| /mf create | mf.create | /mf create (name) | Create your faction. |
| /mf declareindependence | mf.declareindependence |  | Declare independence from your liege. |
| /mf declarewar | mf.declarewar | /mf declarewar <reason> | Declare war on another faction|
| /mf demote | mf.demote | /mf demote <user> | Demotes an officer to member status |
| /mf desc | mf.desc | /mf desc <description> | Set your faction's description. |
| /mf disband | mf.disband | /mf disband | Disband your faction. (must be the owner) |
| /mf duel challenge | mf.duel | /mf duel challenge (player) (time in seconds) | Challenge a player to a duel. |
| /mf duel accept | mf. duel | /mf duel accept (optional:player) | Accept a duel, or a specific players duel. |
| /mf duel cancel | mf.duel | /mf duel cancel | Cancel a duel. |
| /mf flags show | mf.flags | /mf flags show | Shows a list of flags for you faction land. |
| /mf flags set | mf.flags | /mf flags set <option> <value> | Set a faction flag. |
| /mf gate create | mf.gate | /mf gate create (optional:name) | Create a gate, optionally with a name. |
| /mf gate cancel | mf.gate | /mf gate cancel | Cancel's the creation of a gate. |
| /mf gate list | mf.gate | /mg gate list | List of your gates. |
| /mf gate name | mf.gate | - | - |
| /mf gate remove | mf.gate | - | Removes a gate |
| /mf grantaccess | mf.grantaccess | Grant someone access to a locked block. |
| /mf grantindependence | mf.grantindependence | - | Grant a vassal faction independence. |
| /mf help | mf.help | /mf help # | Shows a list of useful commands. |
| /mf home | mf.home | /mf home | Teleport to your faction home. |
| /mf sethome | mf.sethome | /mf sethome | Set your faction's home. |
| /mf info | mf.info | /mf info (optional:faction) | See a factions information. |
| /mf invite | mf.invite | /mf invite <player> | Invite a player to your faction. |
| /mf invoke | mf.invoke | /mf invoke <ally> <enemy> | Call an allied faction into war. |
| /mf join | mf.join | /mf join <faction> | Join a faction to which you were invited to. |
| /mf kick | mf.kick | /mf kick <player> | Kick a player from your faction. |
| /mf leave | mf.leave | /mf leave | Leave your current faction. |
| /mf list | mf.list | /mf list | List all factions on the server. |
| /mf lock | mf.lock | /mf lock | Locks the clicked chest, door, or gate. |
| /mf makepeace | mf.makepeace | /mf makepeace <faction> | Send a peace offering to another faction. |
| /mf map | mf.map | /mf map | Display a map of claims nearby. |
| /mf members | mf.members | /mf members (optional:faction) | List the members of your faction or another faction. |
| /mf power | mf.power | /mf power | Check your power level. |
| /mf prefix | mf.prefix | /mf prefix <prefix> | Set your faction's prefix. |
| /mf promote | mf.promote | /mf promote <player> | Promote a player to officer status. |
| /mf rename | mf.rename | /mf rename <new name> | Rename your faction. |
| /mf revokeaccess | mf.revokeaccess | /mf revokeaccess <player> | Revoke a player's access to a locked block. |
| /mf swearfealty | mf.swearfealty | /mf swearfealty <faction> | Swear fealty to a faction. |
| /mf transfer | mf.transfer | /mf transfer <player> | Transfer faction ownership to another player. |
| /mf unclaim | mf.unclaim | /mf unclaim | Unclaim a chunk of land for your faction. |
| /mf unclaimall | mf.unclaimall | /mf unclaimall | Unclaim all land for your faction. |
| /mf unlock | mf.unlock | /mf unlock | Unlock a chest, door, or gate. |
| /mf vassalize | mf.vassalize | /mf vassalize <faction> | Offer to vassalize a faction. |
| /mf who | mf.who | /mf who <player> | View faction specific info about a player. |

### List of Admin Commands ###
| Command  | Permission | Usage | Description |
| ------------- | ------------- | ------------- | ------------- |
| /mf bypass | mf.bypass | /mf bypass | Toggles bypass faction protections. |