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
4) (Optional) Configure your preferred storage backend in `config.yml` (database or JSON).

### Supported Minecraft Versions
Medieval Factions is supported on the Minecraft versions listed in [`minecraft-versions.json`](minecraft-versions.json): currently **1.19.4**, **1.21.11** and **26.2** (Spigot and its forks). Every stable release is booted on a real server of each of these versions before it is published, and every build checks that the plugin only uses Bukkit API that exists on all of them ([API-compatibility check](https://github.com/Dans-Plugins/release-gates#api-compatibility-check)). Other versions from 1.19.4 onwards are expected to work but are not tested. To support another version, add it to the file: both checks pick it up.

### Upgrading from 5.8.x
**If you run 5.8.x, please upgrade.** Medieval Factions 5.8.x does not close its embedded H2 database cleanly when the server stops. Each restart can leave `medieval_factions_db.mv.db` a little more damaged, until the plugin no longer loads with `MVStoreException: Unable to read the page` ([#1975](https://github.com/Dans-Plugins/Medieval-Factions/issues/1975)). Servers that restart often, such as hosts that stop empty servers, hit this sooner. From 6.0.0 onward, Medieval Factions closes the database explicitly on shutdown.

1. Stop the server and back up `plugins/MedievalFactions/` (in particular `medieval_factions_db.mv.db`).
2. Replace the jar with the [latest release](https://github.com/Dans-Plugins/Medieval-Factions/releases/latest) and start the server.

Before each release, the release gates checked that it loads data written by the previous one: 6.0.0 loads 5.8.1 data, 6.1.0 loads 6.0.0 data, and 7.0.0 loads 6.1.0 data, on both the H2 and JSON backends. A direct jump from 5.8.1 to 7.0.0 has not been gated, which is one more reason to keep the backup. If the plugin already fails with the error above, keep a copy of the damaged file before removing anything. It can sometimes be recovered; ask in [#1975](https://github.com/Dans-Plugins/Medieval-Factions/issues/1975) or on Discord.

### Storage Options
Medieval Factions supports two data storage backends:
- **Database Storage** (default) - Uses embedded H2, or a MariaDB/MySQL server
- **JSON Storage** - Stores data in JSON files for simpler setups

See [Configuration Guide](CONFIG.md#storage-configuration) for details on choosing and configuring your storage backend.

### Web Maps
- **Dynmap**: built in. Install [Dynmap](https://www.spigotmc.org/resources/dynmap.274/) and claimed land appears on its map.
- **BlueMap**: install [BlueMap](https://bluemap.bluecolored.de/) together with the [Bluemap_MedievalFactions](https://github.com/Dans-Plugins/Bluemap_MedievalFactions) add-on, and faction claims are drawn as overlays on the BlueMap web map.

### Expansions
Each of these add-ons requires Medieval Factions and adds something to it. The current stable release of every one was enabled against Medieval Factions 7.0.0 by the [dependents gate](https://github.com/Dans-Plugins/release-gates/actions/runs/36957984824) before 7.0.0 was published.

| Expansion | What it adds | Verified with MF 7.0.0 |
|---|---|---|
| [Currencies](https://github.com/Dans-Plugins/Currencies) ([SpigotMC](https://www.spigotmc.org/resources/96381/)) | Factions mint their own currencies, for local economies | v3.0.0 |
| [Fiefs](https://github.com/Dans-Plugins/Fiefs) ([SpigotMC](https://www.spigotmc.org/resources/98559/)) | Sub-factions (fiefs) inside a faction | v0.12.1 |
| [Democracy](https://github.com/Dans-Plugins/Democracy) ([SpigotMC](https://www.spigotmc.org/resources/139167/)) | Elections for faction leadership | v0.3.0 |
| [Bluemap_MedievalFactions](https://github.com/Dans-Plugins/Bluemap_MedievalFactions) | Faction claims on a [BlueMap](https://bluemap.bluecolored.de/) web map | v1.0 |

Currencies 2.x does not enable on Medieval Factions 5.8 or newer. Use Currencies 3.0.0 or later.

### Works Well With
- [Medieval Roleplay Engine](https://github.com/Dans-Plugins/Medieval-Roleplay-Engine) ([SpigotMC](https://www.spigotmc.org/resources/79993/)): character cards, local chat, emotes and messenger birds. It is a companion plugin for faction roleplay and runs alongside Medieval Factions.
- [Mailboxes](https://github.com/Dans-Plugins/Mailboxes) ([SpigotMC](https://www.spigotmc.org/resources/96611/)): persistent messages. When it is installed, Medieval Factions delivers faction notifications through it.
- [Medieval Economy](https://github.com/Dans-Plugins/Medieval-Economy) ([SpigotMC](https://www.spigotmc.org/resources/81836/)): a coinpurse and a physical currency item.

Every plugin is listed on [dansplugins.com](https://dansplugins.com), and [Dan's Plugin Manager](https://github.com/Dans-Plugins/Dans-Plugin-Manager) can install them in game with `/dpm get <plugin>`.

## Usage

### Documentation
- [User Guide](USER_GUIDE.md) - Getting started and common scenarios
- [FAQ](FAQ.md) - Answers to frequently asked questions
- [Commands Reference](COMMANDS.md) - Complete list of all commands
- [Configuration Guide](CONFIG.md) - Detailed config options
- [Faction Flags](FACTION_FLAGS.md) - Faction flag reference
- [Database Querying Guide](DATABASE_QUERYING.md) - How to query the database directly
- [REST API Usage Guide](API_USAGE.md) - The opt-in HTTP API for integrating other plugins and tools

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
- [CONTRIBUTING.md](CONTRIBUTING.md)
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

To build directly on Medieval Factions, the [expansions above](#expansions) are working examples. Their source shows how each one hooks into Medieval Factions' services, events and permissions. A clearer API for expansion permissions is tracked in [#1683](https://github.com/Dans-Plugins/Medieval-Factions/issues/1683).

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

## Usage reporting

Usage reporting is on by default: when the plugin is enabled, and each time one of its commands is used, it sends its name, version and the command's name (`startup` and `command` events; aliases such as `/mf` report under the command's declared name) to https://trace.danielstephenson.dev so it is known which plugins are actually in use. Nothing about players, worlds, IPs or the server is sent, and nothing typed after a command. The plugin says on every startup whether reporting is on. To turn it off:

- `usage-reporting.enabled: false` in this plugin's `config.yml`
- for every plugin on the server that reports to trace: `enabled: false` in `plugins/trace/config.yml` (written by the first such plugin to start)
- the environment variable `TRACE_USAGE_REPORTING=off` or `DO_NOT_TRACK=1`

Details: https://github.com/Stephenson-Software/trace#usage-reporting

## Update check

Once per startup, off the main thread, the plugin asks GitHub for the latest published Medieval Factions release: one unauthenticated `GET https://api.github.com/repos/Dans-Plugins/Medieval-Factions/releases/latest`, which returns the release's tag and link. If that release is newer than the version the server runs, one line goes to the console, and players with the `mf.updatenotice` permission (ops by default) see it when they join. The line names the new version, links the release, and gives a reason: a server on a version before 6.0.0 is told that the release fixes database corruption on shutdown (H2); otherwise it points at the release notes. Development builds such as `7.0.1-SNAPSHOT` are never told to "upgrade" to an older release, and pre-releases are never offered.

Nothing about the server or its players is sent (the request carries only a `User-Agent` naming the plugin and its version, which GitHub requires), and nothing is downloaded or installed. A failed check (offline, timeout, rate limit) is silent and never delays startup.

The check is on by default. To turn it off:

- `update-check.enabled: false` in this plugin's `config.yml`
- the environment variable `TRACE_USAGE_REPORTING=off` or `DO_NOT_TRACK=1`, the same switches that turn off usage reporting
