# Medieval Factions - SpigotMC Page Content

This document contains the formatted content for the Medieval Factions SpigotMC page. The content below can be copied and pasted directly to SpigotMC with minimal formatting adjustments.

---

## 📜 Overview

Medieval Factions is a comprehensive system of mechanics that allows for the simulation of sovereign nations in Minecraft. Players can create nations, claim territory, engage in warfare or politics, write laws, hold dueling tournaments, and generally attempt to recreate society in a medieval setting.

**Key Features:**
- Create and manage factions with customizable roles and permissions
- Claim and protect territory using a power-based system
- Engage in diplomacy: form alliances, declare wars, establish vassalages
- Build gates that can be opened and closed on command
- Lock blocks to prevent theft and unauthorized access
- Comprehensive dueling system for honorable combat
- Faction laws and governance systems
- Full Dynmap integration for territory visualization
- PlaceholderAPI support for custom displays
- Multi-language support (English, Spanish, Russian, Portuguese, German, Dutch)

The development of the fifth major version of MF was led by [alyphen](https://github.com/alyphen) (creator of [RPKit](https://github.com/RP-Kit/RPKit/wiki)). Huge thanks to her!

---

## ✨ Features

### Core Faction System
- **Create Factions:** Establish your own nation with customizable names, descriptions, and colors
- **Power System:** Dynamic power-based land claiming where faction power equals the sum of all member power
- **Territory Claims:** Claim chunks to protect your land from other factions
- **Roles & Permissions:** Create custom roles with granular permission control
- **Member Management:** Invite, kick, and manage faction members with application system

### Diplomacy & Warfare
- **Alliances:** Form strategic partnerships with other factions
- **Wars:** Declare war on rival factions with full combat mechanics
- **Vassalage System:** Establish feudal relationships as liege or vassal
- **Power Contributions:** Vassals contribute power to their lieges
- **Independence:** Vassals can declare independence, triggering automatic war

### Territory Management
- **Multiple Claim Modes:**
  - Single chunk claiming
  - Radius claiming for large areas
  - Auto-claim mode for efficient expansion
  - Fill claim to complete bordered territories
- **Faction Homes:** Set and teleport to faction headquarters
- **Territory Indicators:** Visual alerts when entering new faction territory (action bar and title)
- **Contiguous Claims:** Optional requirement for connected territories

### Advanced Features
- **Gates:** Create mechanical gates that faction members can toggle open/closed
- **Block Locking:** Lock chests, doors, and other blocks with access control lists
- **Faction Laws:** Write and enforce faction rules and regulations
- **Chat Channels:** Dedicated chat for faction, allies, and vassals
- **Dueling System:** Challenge players to timed honorable combat
- **Faction Flags:** Per-faction settings for customizing behavior

### Integrations
- **Dynmap:** View faction territories on dynamic web maps
- **PlaceholderAPI:** Display faction information in other plugins
- **FactionsBridge:** Compatibility layer for cross-faction-plugin addons

### Protection & Security
- **Territory Protection:** Prevent outsiders from building, breaking, or interacting
- **Mob Protection:** Optional protection from hostile mob damage by non-members
- **Villager Trade Protection:** Protect villager trading halls
- **Configurable PVP:** War-based PVP, friendly fire control, and more

---

## 📥 Installation

### First Time Installation
1. Download the latest version from the [Updates tab](https://www.spigotmc.org/resources/medieval-factions-sovereign-nation-simulator.79941/updates)
2. Place the `.jar` file in your server's `plugins` folder
3. Restart your server
4. Configure the plugin in `plugins/MedievalFactions/config.yml` (optional)

### Requirements
- **Minecraft Version:** 1.16.5+ (check specific version compatibility)
- **Server Software:** Spigot, Paper, or any Spigot-based server
- **Java Version:** Java 8 or higher

### Optional Integrations

**Dynmap Integration**
To view claimed land on a dynamic map, download and install [Dynmap](https://www.spigotmc.org/resources/dynmap.274/). Medieval Factions will automatically detect and integrate with Dynmap.

**PlaceholderAPI**
For displaying faction information in other plugins, install [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/). See the [Placeholders Wiki](https://github.com/dmccoystephenson/Medieval-Factions/wiki/Placeholders) for available placeholders.

### Expansions
- **[Fiefs](https://github.com/dmccoystephenson/Fiefs)** - Additional faction features
- **[Currencies](https://github.com/dmccoystephenson/Currencies)** - Economy system for factions

---

## 📚 Documentation

Medieval Factions has comprehensive documentation available on GitHub:

### Quick Start Guides
- **[User Guide](https://github.com/Dans-Plugins/Medieval-Factions/blob/master/USER_GUIDE.md)** - Getting started, common scenarios, and step-by-step tutorials
- **[Commands Reference](https://github.com/Dans-Plugins/Medieval-Factions/blob/master/COMMANDS.md)** - Complete list of all commands with descriptions and permissions
- **[Configuration Guide](https://github.com/Dans-Plugins/Medieval-Factions/blob/master/CONFIG.md)** - Detailed server configuration options
- **[Faction Flags Reference](https://github.com/Dans-Plugins/Medieval-Factions/blob/master/FACTION_FLAGS.md)** - Per-faction settings and customization

### Wiki Resources
- **[Wiki Guide](https://github.com/dmccoystephenson/Medieval-Factions/wiki/Guide)** - Comprehensive wiki documentation
- **[FAQ](https://github.com/dmccoystephenson/Medieval-Factions/wiki/FAQ)** - Frequently asked questions
- **[Features List](https://github.com/dmccoystephenson/Medieval-Factions/wiki/Features)** - Complete feature list
- **[Placeholders](https://github.com/dmccoystephenson/Medieval-Factions/wiki/Placeholders)** - PlaceholderAPI integration

### Developer Resources
- **[External API Documentation](https://github.com/dmccoystephenson/Medieval-Factions/wiki/External-API-Documentation)** - For creating add-ons
- **[Developer Notes](https://github.com/dmccoystephenson/Medieval-Factions/wiki/Developer-Notes)** - Technical documentation
- **[Contributing Guide](https://github.com/Dans-Plugins/Medieval-Factions/blob/master/CONTRIBUTING.md)** - How to contribute to the project

---

## 🎮 Quick Start

### For Players

**1. Create Your First Faction**
```
/f create YourFactionName
```

**2. Claim Your Territory**
```
/f claim
```
Stand in a chunk and run this command to claim it. You can also use:
- `/f claim 2` - Claim in a 2-chunk radius (5x5 area)
- `/f claim auto` - Toggle auto-claiming as you walk

**3. Set Your Faction Home**
```
/f sethome
```
Must be used in your claimed territory. Teleport back anytime with `/f home`

**4. Invite Members**
```
/f invite PlayerName
```
Grow your faction by inviting trusted players

**5. Customize Your Faction**
```
/f flag set color #FF0000
/f set description "Your faction description"
/f law add "Your faction rules"
```

### For Server Owners

**Basic Configuration:**
Edit `plugins/MedievalFactions/config.yml` to customize:
- Power system (initial power, max power, power loss/gain)
- PVP settings (war requirements, friendly fire, etc.)
- Territory settings (claim limits, contiguous claims)
- Faction limits (max members, max name length)
- Dynmap integration settings
- Language settings

**Recommended Settings for Different Server Types:**

*Peaceful RP Server:*
```yaml
pvp.warRequiredForPlayersOfDifferentFactions: true
pvp.friendlyFire: false
factions.allowNeutrality: true
```

*Hardcore PVP Server:*
```yaml
pvp.warRequiredForPlayersOfDifferentFactions: false
pvp.enableWartimeBlockDestruction: true
players.powerLostOnDeath: 2
factions.zeroPowerFactionsGetDisbanded: true
```

See the [Configuration Guide](https://github.com/Dans-Plugins/Medieval-Factions/blob/master/CONFIG.md) for complete configuration documentation.

---

## 🎯 Use Cases

Medieval Factions is perfect for:
- **Survival Servers:** Protect bases and create player nations
- **Roleplay Servers:** Simulate medieval kingdoms and politics
- **PVP Servers:** Strategic faction warfare with territory control
- **Towny-Style Servers:** Alternative to Towny with unique features
- **Nation RP:** Create complex political systems with vassalage
- **Community Servers:** Foster cooperation and community building

---

## 🛠️ Support

### Need Help?
- **Discord Server:** [Join our Discord](https://discord.gg/xXtuAQ2) for support and discussion
- **Bug Reports:** [Submit issues on GitHub](https://github.com/dmccoystephenson/Medieval-Factions/issues/new/choose)
- **Known Issues:** [View known bugs](https://github.com/dmccoystephenson/Medieval-Factions/issues?q=is%3Aopen+is%3Aissue+label%3Abug)

### For Developers
- **Creating Add-ons:** Use [FactionsBridge](https://www.spigotmc.org/resources/factionsbridge.89716/) for cross-plugin compatibility
- **External API:** [API Documentation](https://github.com/dmccoystephenson/Medieval-Factions/wiki/External-API-Documentation)
- **Contributing:** [Contribution Guidelines](https://github.com/Dans-Plugins/Medieval-Factions/blob/master/CONTRIBUTING.md)

---

## 🌐 Multi-Language Support

Medieval Factions is available in multiple languages:
- English (en-US)
- Spanish (es-ES)
- Russian (ru-RU)
- Brazilian Portuguese (pt-BR)
- German (de-DE)
- Dutch (nl-NL)

Change the language in `config.yml`:
```yaml
language: en-US
```

---

## 📊 Statistics

View server statistics and usage on [bStats](https://bstats.org/plugin/bukkit/Medieval%20Factions/8929)

---

## 👥 Credits

### Lead Developers
- **Daniel Stephenson** - Original creator
- **[Ren Binden (alyphen)](https://github.com/alyphen)** - Created Medieval Factions 5
- **[Pasarus](https://github.com/Pasarus)** - Storage system overhaul
- **Caibinus** - Duels, gates, and Dynmap integration
- **[Callum](https://www.spigotmc.org/resources/authors/retrix_solutions.491191/)** - Command system overhaul, events, PlaceholderAPI

### Contributors
Special thanks to all contributors including: Richardhyy, Mitras2, Kaonami, GoodLucky777, Elafir, Deej, VoChiDanh, Kyrenic, Tems, MestreWilll, and many others.

### Translators
- **Spanish:** Khanter, Neh, Johnny, lilhamoood
- **Russian:** 1barab1, 2kManfridi
- **Portuguese:** Kaonami, MestreWilll
- **German:** graffity_X
- **Dutch:** JustGllenn, TDL, n0virus

See the [README](https://github.com/Dans-Plugins/Medieval-Factions/blob/master/README.md) for the complete credits list.

---

## 📜 License

Medieval Factions is licensed under the [GNU General Public License v3.0](https://github.com/Dans-Plugins/Medieval-Factions/blob/master/LICENSE) (GPL-3.0).

You are free to use, modify, and distribute this software, provided that:
- Source code is made available under the same license when distributed
- Changes are documented and attributed
- No additional restrictions are applied

---

## 🔗 Links

- **GitHub Repository:** [https://github.com/Dans-Plugins/Medieval-Factions](https://github.com/Dans-Plugins/Medieval-Factions)
- **Discord Server:** [https://discord.gg/xXtuAQ2](https://discord.gg/xXtuAQ2)
- **SpigotMC Resource:** [https://www.spigotmc.org/resources/medieval-factions.79941/](https://www.spigotmc.org/resources/medieval-factions.79941/)
- **bStats:** [https://bstats.org/plugin/bukkit/Medieval%20Factions/8929](https://bstats.org/plugin/bukkit/Medieval%20Factions/8929)

---

## ⭐ Why Choose Medieval Factions?

- **Active Development:** Regularly updated with new features and bug fixes
- **Comprehensive Features:** Everything you need for faction gameplay
- **Highly Configurable:** Customize every aspect to fit your server
- **Excellent Documentation:** Extensive guides and API documentation
- **Community Support:** Active Discord community and developer support
- **Open Source:** GPL-3.0 licensed with public GitHub repository
- **Multi-Language:** Supports 6+ languages
- **Integration Ready:** Works with Dynmap, PlaceholderAPI, and more
- **Proven Track Record:** Used on hundreds of servers since 2020

---

## 📝 Version History

For detailed version history and changelog, see the [Updates tab](https://www.spigotmc.org/resources/medieval-factions-sovereign-nation-simulator.79941/updates).

The first release version (v1.7) was released in June 2020. The plugin has evolved significantly since then, with the current version featuring a complete rewrite for enhanced performance and functionality.

---

*Medieval Factions - Build your empire, forge alliances, and create your own medieval story!*
