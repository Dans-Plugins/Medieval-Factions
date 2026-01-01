# Medieval Factions

## Description 
Medieval Factions is a system of mechanics that allows for the simulation of sovereign nations in Minecraft. Players can create nations, claim territory, engage in warfare or politics, write laws or hold dueling tournaments, and generally are able to attempt to recreate society somewhat.

A list of features can be found [here](https://github.com/Dans-Plugins/Medieval-Factions/wiki/Features).

The development of the fifth major version of MF was led by [alyphen](https://github.com/alyphen) (creator of [RPKit](https://github.com/RP-Kit/RPKit/wiki)). Huge thanks to her!

## Installation
### First Time Installation
1) You can download the plugin from [this page](https://www.spigotmc.org/resources/medieval-factions-sovereign-nation-simulator.79941/updates).
2) Once downloaded, place the jar in the plugins folder of your server files.
3) Restart your server.

### Dynmap Integration
Dynmap has been integrated with this plugin. In order to be able to view claimed land on a dynamic map, download and install the plugin [here](https://www.spigotmc.org/resources/dynmap.274/).

### Expansions
1) [Fiefs](https://github.com/Dans-Plugins/Fiefs)
2) [Currencies](https://github.com/Dans-Plugins/Currencies)

## Usage

### Documentation
- [User Guide](USER_GUIDE.md) - Getting started and common scenarios
- [Commands Reference](COMMANDS.md) - Complete list of all commands
- [Configuration Guide](CONFIG.md) - Detailed config options
- [Faction Flags](FACTION_FLAGS.md) - Faction flag reference

### Wiki & Additional Resources
- [Wiki Guide](https://github.com/Dans-Plugins/Medieval-Factions/wiki/Guide)
- [FAQ](https://github.com/Dans-Plugins/Medieval-Factions/wiki/FAQ)
- [List of Placeholders](https://github.com/Dans-Plugins/Medieval-Factions/wiki/Placeholders)

## Support
You can find the support discord server [here](https://discord.gg/xXtuAQ2).

### Experiencing a bug?
Please fill out a bug report [here](https://github.com/Dans-Plugins/Medieval-Factions/issues/new/choose).

- [Known Bugs](https://github.com/Dans-Plugins/Medieval-Factions/issues?q=is%3Aopen+is%3Aissue+label%3Abug)

## Contributing
- [Contributing.md](https://github.com/Dans-Plugins/Medieval-Factions/blob/main/CONTRIBUTING.md)
- [Notes for Developers](https://github.com/Dans-Plugins/Medieval-Factions/wiki/Developer-Notes)

## Testing
### Unit Tests
To run the unit tests, you can use the following command:

Linux:
```bash
./gradlew clean test
```
Windows:
```cmd
.\gradlew.bat clean test
```

If you see BUILD SUCCESSFUL, then the tests have passed.

### Looking to create an add-on plugin?
I recommend using [FactionsBridge](https://www.spigotmc.org/resources/factionsbridge.89716/) by [Retrix_Solutions](https://www.spigotmc.org/resources/authors/retrix_solutions.491191/). It would make your add-on plugin usable across a number of factions implementations.

Alternatively, you can use the External API, the documentation for which can be found [here](https://github.com/Dans-Plugins/Medieval-Factions/wiki/External-API-Documentation).

## Development
### Test Server with Plugin Hot-Reloading
For development purposes, a Docker-based test server is available with integrated plugin hot-reloading capabilities using ServerUtils (a modern Plugman alternative).

#### Setup
1. Copy `sample.env` to `.env` and configure as needed
2. Build the plugin: `./gradlew build`
3. Start the test server: `./up.sh`

#### Plugin Hot-Reloading
After making changes to the plugin code, you can quickly reload it without restarting the server:

**Option 1: Using the reload script (recommended)**
```bash
./reload-plugin.sh
```

**Option 2: Manual reload**
1. Build the plugin: `./gradlew build`
2. Copy the new jar to the running container
3. Use ServerUtils commands in-game or console:
   - `/serverutils reload MedievalFactions` - Reload the plugin
   - `/serverutils unload MedievalFactions` - Unload the plugin
   - `/serverutils load MedievalFactions` - Load the plugin
   - `/serverutils list` - List all plugins

This significantly speeds up the development cycle by eliminating the need for full server restarts during testing.

#### Stopping the Test Server
```bash
./down.sh
```

## Authors and acknowledgement
### Developers
| Name                                                                          | Main Contributions                                                                                                                          |
|-------------------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------|
| Daniel Stephenson                                                             | Original creator                                                                                                                            |
| [Ren Binden](https://github.com/alyphen)                                      | Created Medieval Faction 5                                                                                                                  |
| [Pasarus](https://github.com/Pasarus)                                         | Overhauled the Storage Manager to use UUIDs and JSON and made other improvements to the plugin                                              |
| Caibinus                                                                      | Implemented Duels, Gates and Dynmap Integration                                                                                             |
| [Callum](https://www.spigotmc.org/resources/authors/retrix_solutions.491191/) | Created event classes, overhauled the Command System, implemented PlaceholderAPI integration and made many other improvements to the plugin |
| Richardhyy                                                                    | Fixed some encoding issues                                                                                                                  |
| Mitras2                                                                       | Implemented ActionBar territory alerts                                                                                                      |
| [Kaonami](https://github.com/Daniels7k)                                       | Fixed a typo in the README                                                                                                                  |
| GoodLucky777                                                                  | Fixed a bug and a few typos in the code                                                                                                     |
| Elafir                                                                        | Made it possible to control gates with redstone                                                                                             |
| [Deej](https://github.com/Mr-Deej)                                            | Added checks to several commands                                                                                                            |
| VoChiDanh                                                                     | Refactored parts the PersistentData class in an attempt to resolve java compatibility issues                                                |
| Kyrenic                                                                       | Implemented contiguous claims config option                                                                                                 |
| [Tems](https://github.com/Tems-py)                                            | Fixed claim protection issues                                                                                                               |
| MestreWilll                                                                   | Contributed Brazilian Portueguese translation                                                                                               |  

### Translators
| Name                                                             | Language(s)          |
|------------------------------------------------------------------|----------------------|
| Khanter                                                          | Spanish              |
| Neh                                                              | Spanish              |
| Johnny                                                           | Spanish              |
| lilhamoood                                                       | Spanish              |
| 1barab1                                                          | Russian              |
| 2kManfridi                                                       | Russian              |
| [Kaonami](https://github.com/Daniels7k)                          | Portuguese Brazilian |
| [graffity_X](https://www.spigotmc.org/members/kicker765.946561/) | German               |
| JustGllenn                                                       | Dutch                |
| TDL                                                              | Dutch                |
| [n0virus](https://www.youtube.com/c/n0virus)                     | Dutch                |
| MestreWilll                                                      | Brazilian Portuguese |   

I created this plugin because I wanted to use the original [Factions](https://www.spigotmc.org/resources/factions.1900/) plugin for an upcoming server of mine, but it wasn't updated for the version of minecraft I was going to be using. I decided to take inspiration from the concept of factions - groups of players that can claim land - and create my own factions plugin.

The first release version, [v1.7](https://github.com/Dans-Plugins/Medieval-Factions/releases/tag/v1.7), was released on SpigotMC in June 2020 and looked much different than the plugin does today.

I am extremely grateful to those that have donated their time improving the project, one way or another. The plugin wouldn't be where it is today without the contributions of others.

## License

This project is licensed under the [GNU General Public License v3.0](LICENSE) (GPL-3.0).

You are free to use, modify, and distribute this software, provided that:
- Source code is made available under the same license when distributed.
- Changes are documented and attributed.
- No additional restrictions are applied.

See the [LICENSE](LICENSE) file for the full text of the GPL-3.0 license.

## Project Status
This project is in active development.

### bStats
You can view the bStats page for the plugin [here](https://bstats.org/plugin/bukkit/Medieval%20Factions/8929).
