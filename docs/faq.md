# Medieval Factions 5 FAQ

## How do I transfer ownership of my faction to another player?
Use the 'role set' command to set their role to owner, then take away your owner role.

## How do I claim more land?
You need to get more power. A faction is able to claim a number of chunks equal to their total power (made up of the power of its members).

## How do I get more power?
You can get more power by spending time on the server and by killing other players.

## Does MF have economic mechanics?
The base plugin has no economic mechanics, but an expansion called Currencies exist that does.

## How do I make claims show up on the dynmap?
If you have dynmap installed, this should happen automatically.

## How do I remove a faction?
This feature will be available in 5.1.0. In that version and onwards, you are able to remove a faction by typing /mf disband <faction>.

## How do I conquer land from another faction?
You can only conquer a faction's land if they have more chunks claimed than total power, so you need to decrease their power. A good way to do this is by killing their players.

## Why isn't the force command working?
The force command was removed in MF 5, the functionality is now available in the normal command (with a specified faction)

## What is this?
Medieval Factions is a system of mechanics that allows for the simulation of sovereign nations. Players can create nations, claim territory, engage in warfare or politics, write laws or hold dueling tournaments, and generally are able to attempt to recreate society somewhat.

## What is a faction?
In this plugin, a faction is considered a **feudal nation** and a nation is considered a **diplomatic, lawful group** of players. Factions are feudal because they can be vassals or lieges, they are diplomatic because they can have allies and enemies, and they are lawful because they can have laws.

## How do I make my faction claim more land?
A faction is able to claim as much land as their cumulative power level allows. The cumulative power level is made up of the sum of the power levels of the members of the faction. This will allow you and your other members to gain more power through killing/scheduled increases which will increase your cumulative power level which is tied to how much land your faction can claim.

## How do I increase the Demesne Limit for my players?
You can alter the initialMaxPowerLevel config option to allow players to gain more power.

## Where are config options found?
Config options can be viewed by typing /mf config show [ 1 | 2 ] in game. Alternatively, they can be found in the config.yml file in the "MedievalFactions" folder located in your "plugins" folder.

## How do I set a config option?
You can set a config option by typing /mf config set (option) (value).

## How do I change the language of the plugin?
To change what language the plugin is using, modify the "languageid" config option to one of the supported languages found in the plugin information outputted when you type /mf with no arguments.

## How do I use a translation file that isn't supported?
To use your own translation for the plugin, rename the en-us.tsv file in the "languages" folder of the "MedievalFactions" folder. Open the file and translate the values for each key accordingly. Then, modify the "languageid" to be your file name without ".tsv". Your translated key values should immediately be used by the plugin.

## Locks aren't working on 1.12.2! What do I do?
This is a known issue, and you can check its progress here.

## What are faction flags?
Faction flags are per-faction configuration options. They allow server owners to customize the functionality of their factions.

## Where are faction flags found?
Faction flags can be viewed by typing /mf flags show in game.

## How do I set a faction flag?
You can set a config option by typing /mf flags set (flag) (value).

## Does Medieval Factions have economic mechanics?
At this time, the plugin does not have economic mechanics. I am considering implementing some, however. I do have another plugin called Medieval Economy that has economic mechanics.

## Does Medieval Factions involve chat formatting?
Medieval Factions does have chat formatting involved, specifically prefixes and faction chat. Prefixes can be disabled through a config option.

## How do you claim land?
You can claim land with the /mf claim command.

## How do I use the Dynmap-related features?
In order to view the territory of factions on a dynmap, you need to have Dynmap installed. Check out [this page](https://github.com/dmccoystephenson/Medieval-Factions/wiki/Dynmap-Installation-&-Configuration) for information on installation and configuration.

### Aternos Users
As far as I know, Aternos does not offer Dynmap. Please let me know if this changes.

## Can you disable faction protections?
As of Medieval Factions v4.3, server owners can disable faction protections by setting the "factionProtectionsEnabled" config option to false.

## Is the plugin economy-based?
Unless you install the Currencies expansion, Medieval Factions does not have any economic mechanics.

## How can I forcefully remove a faction?
You can remove a faction by typing /mf disband 'faction name'. This command was implemented before the force command was created.

## How do I change the color of my claimed territory on the Dynmap?
You can change the color that your territory shows up as by setting the "dynmapTerritoryColor" faction flag. See the page on faction flags [here](https://github.com/dmccoystephenson/Medieval-Factions/wiki/Faction-Flags).

## Can you view the members of your faction on the Dynmap?
No. If you click on a faction's territory, you can see their population but not who is in their faction, currently.

## Can you view the vassals of your faction on the Dynmap?
Yes. If you click on a faction's territory, you can see a list of their vassals.

## How do you make another faction your vassal?
You can offer vassalization to other factions by typing /mf vassalize 'faction name'.

## What happens when a faction declares independence from their liege?
When a faction declares independence, they're breaking a contract they made with their liege. Immediately afterwards, they'll be at war with their prior liege. Their prior liege will then be able to invoke their alliances with other factions to get them to go to war with them as well.

## How do I get more power?
Currently, there are two ways you can increase your individual power level. You can either be online for the scheduled power increase, or you can kill other players.

## How does conquering land from other factions work?
When a faction has more claimed chunks than power, their chunks are conquerable. An officer or the owner of an enemy faction is able to type /mf claim and claim a chunk of their land. A faction's power can be decreased by killing its members or waiting for the faction to go inactive.