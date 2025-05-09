# What are faction flags?
Faction flags are per-faction configuration options. They allow faction owners to customize the functionality of their factions.

# Where are faction flags found?
Faction flags can be viewed by typing /mf flags show in game.

# How do I set a faction flag?
You can set a faction flag by typing /mf flags set (flag) (value).

# What colors are accepted as values?
* BLACK("#000000"), // Black
* DARK_BLUE("#151B8D"), // Denim Dark Blue
* DARK_GREEN("#254117"), // Dark Forest Green
* DARK_AQUA("#348781"), // Medium Aquamarine
* DARK_RED("#990012"), // Red Wine
* DARK_PURPLE("#461B7E"), // Purple Monster
* DARK_GRAY("#736F6E"), // Gray
* GOLD("#FDD017"), // Bright Gold
* GRAY("#B6B6B4"), // Gray Cloud
* BLUE("#1569C7"), // Blue Eyes
* GREEN("#41A317"), // Lime Green
* AQUA("#00FFFF"), // Cyan or Aqua
* RED("#FF0000"), // Red
* LIGHT_PURPLE("#FF00FF"), // Magenta
* YELLOW("#FFFF00"), // Yellow
* WHITE("#FFFFFF"), // White

# Faction Flag Descriptions
Name | Description | Notes
------------ | ------------- | ------------
mustBeOfficerToManageLand | When enabled, a player must be an officer to claim and unclaim land. |
mustBeOfficerToInviteOthers | When enabled, a player must be an officer to invite other players to the faction. |
alliesCanInteractWithLand | When enabled, players in allied factions can interact with land claimed by the faction. | The default value of this faction flag is dependent on an associated config option. 
vassalageTreeCanInteractWithLand | When enabled, players in factions in the faction's vassalage tree can interact with land claimed by the faction. | The default value of this faction flag is dependent on an associated config option. 
neutral | When enabled, the faction is prevented from declaring war and protected from being declared war on. | This faction flag is only available if an associated config option is enabled.
dynmapTerritoryColor | Color of claimed land on the Dynmap. |
territoryAlertColor | Color of territory alert. |
prefixColor | Color of faction chat prefix. | This faction flag is only available if the "playersChatWithPrefixes" config option is set to true.
allowFriendlyFire | When enabled, friendly fire is allowed |