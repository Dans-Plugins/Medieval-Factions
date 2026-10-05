# Who can do what

What players can and cannot do in faction territory, **observed by bots on a real server**, not written by hand. Before each release, and on pull requests that touch protection code, the behaviour gate (release-gates `behaviour-gate.yml`, rows in [Dans-Plugins/plugin-fixtures](https://github.com/Dans-Plugins/plugin-fixtures/blob/main/scenarios/medieval-factions-behaviour.json)) plays every row below on the stable release and on the candidate, and reports any row that changed.

This page was generated from release-gates run [37257995448](https://github.com/Dans-Plugins/release-gates/actions/runs/37257995448) (the current development build, Minecraft 26.1). It is regenerated when the table or the plugin's behaviour changes. For what each option means, see [CONFIG.md](CONFIG.md).

**Roles** (Alpha owns every claimed arena):

| role | who |
|---|---|
| owner | Alpha's owner |
| member | a member of Alpha |
| ally | the owner of Bravo, allied with Alpha both ways |
| enemy | the owner of Echo, no relationship to Alpha |
| enemyAtWar | the owner of Whiskey, at war with Alpha |
| stranger | a player in no faction |
| bypass | a server operator with `/f bypass` on |

**Arenas:** `ownerClaim` is a chunk Alpha claims; `lockArena` is a second Alpha chunk with a chest Alpha's owner has locked and given the ally access to; `wilderness` is unclaimed.

✅ took effect · ❌ refused · ✅⚠ took effect but the player was told it was refused · · not observed.

## ownerClaim (claimed by Alpha) — default config

| action | member | ally | enemy | enemyAtWar | stranger | bypass | owner |
|---|---|---|---|---|---|---|---|
| flint and steel on stone | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ |  |
| cobblestone on stone | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ |  |
| break dirt | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ |  |
| right-click chest | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ |  |
| bone meal on grass patch |  |  | ❌ |  |  |  |  |
| item frame on stone |  |  | ❌ |  |  |  |  |
| armor stand on stone |  |  | ❌ |  |  |  |  |
| end crystal on obsidian |  |  | ❌ |  |  |  |  |
| water bucket on stone |  |  | · |  |  |  |  |
| snowball on stone |  |  | ✅ |  |  |  |  |
| snowball on stone (last item) |  |  | ✅ |  |  |  |  |
| egg on stone |  |  | ✅ |  |  |  |  |
| experience bottle on stone |  |  | ✅ |  |  |  |  |
| splash potion on stone |  |  | ✅ |  |  |  |  |
| lingering potion on stone |  |  | ✅ |  |  |  |  |
| splash potion on chest |  |  | ✅ |  |  |  |  |
| right-click stone |  |  | ❌ |  |  |  |  |
| right-click oak door |  |  | ❌ |  |  |  |  |
| use diamond on item frame |  |  | ❌ |  |  |  |  |
| use leather helmet on armor stand |  |  | ❌ |  |  |  |  |
| right-click chest minecart |  |  | ❌ |  |  |  |  |
| ladder on stone |  |  | ❌ | ✅ |  |  |  |
| hit the owner | ❌ | ❌ | ❌ | ✅ | ✅ |  |  |
| hit the stranger |  |  |  |  |  |  | ✅ |

## wilderness (wilderness) — default config

| action | stranger |
|---|---|
| flint and steel on stone | ✅ |
| right-click chest | ✅ |

## lockArena (claimed by Alpha) — default config

| action | member | ally | enemy | stranger | bypass |
|---|---|---|---|---|---|
| right-click chest | ❌ | ✅ | ❌ | ❌ | ✅ |
| break chest | ❌ |  | ❌ |  |  |

## ownerClaim (claimed by Alpha) — `factions.nonMembersCanInteractWithDoors: True`

| action | enemy |
|---|---|
| right-click oak door | ✅ |
| sneaking: flint and steel on oak door | ❌ |

## wilderness (wilderness) — `wilderness.interaction.prevent: True`

| action | stranger |
|---|---|
| flint and steel on stone | ❌ |
| right-click chest | ❌ |

## ownerClaim (claimed by Alpha) — `factions.wartimePlaceableBlocks: ['COBBLESTONE', 'FLINT_AND_STEEL', 'BONE_MEAL', 'ARMOR_STAND']`, `factions.wartimeBreakableBlocks: ['DIRT']`, `factions.wartimeInteractableBlocks: ['CHEST', 'OAK_DOOR']`

| action | enemyAtWar | enemy |
|---|---|---|
| cobblestone on stone | ✅ | ❌ |
| flint and steel on stone | ❌ | ❌ |
| bone meal on grass patch | ✅ | ❌ |
| armor stand on stone | ✅ | ❌ |
| break dirt | ✅ | ❌ |
| right-click chest | ✅ | ❌ |
| right-click oak door | ✅ | ❌ |

## wilderness (wilderness) — `wilderness.place.prevent: True`, `wilderness.break.prevent: True`

| action | stranger |
|---|---|
| cobblestone on stone | ❌ |
| break dirt | ❌ |

## ownerClaim (claimed by Alpha) — `factions.nonMembersCanInteractWithEntities: True`

| action | enemy |
|---|---|
| use diamond on item frame | ✅ |
| use leather helmet on armor stand | ✅ |
| right-click chest minecart | ✅ |

## ownerClaim (claimed by Alpha) — `factions.wartimePlaceableBlocks: ['FLINT_AND_STEEL', 'FIRE']`

| action | enemyAtWar |
|---|---|
| flint and steel on stone | ✅ |

## ownerClaim (claimed by Alpha) — `pvp.friendlyFire: True`

| action | member |
|---|---|
| hit the owner | ✅ |

## ownerClaim (claimed by Alpha) — `pvp.warRequiredForPlayersOfDifferentFactions: False`

| action | enemy |
|---|---|
| hit the owner | ✅ |

## ownerClaim (claimed by Alpha) — `pvp.enabledForFactionlessPlayers: False`

| action | stranger | owner |
|---|---|---|
| hit the owner | ❌ |  |
| hit the stranger |  | ❌ |
