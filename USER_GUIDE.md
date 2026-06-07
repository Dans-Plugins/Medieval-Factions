# Medieval Factions - User Guide

This guide provides step-by-step instructions for common scenarios and getting started with Medieval Factions.

## Table of Contents
- [Getting Started](#getting-started)
- [Common Scenarios](#common-scenarios)
  - [Creating Your First Faction](#creating-your-first-faction)
  - [Joining an Existing Faction](#joining-an-existing-faction)
  - [Claiming Your First Territory](#claiming-your-first-territory)
  - [Inviting Members](#inviting-members)
  - [Setting Up Faction Roles](#setting-up-faction-roles)
  - [Forming an Alliance](#forming-an-alliance)
  - [Declaring War](#declaring-war)
  - [Establishing a Vassalage](#establishing-a-vassalage)
  - [Creating and Using Gates](#creating-and-using-gates)
  - [Locking Important Blocks](#locking-important-blocks)
  - [Managing Faction Laws](#managing-faction-laws)
  - [Configuring Faction Settings](#configuring-faction-settings)
- [Advanced Features](#advanced-features)
- [Tips and Best Practices](#tips-and-best-practices)
- [Troubleshooting](#troubleshooting)

## Getting Started

### Understanding the Basics

Medieval Factions allows you to:
- **Create or join factions** - Groups of players working together
- **Claim territory** - Protect land from other players
- **Engage in diplomacy** - Form alliances, declare wars, establish vassalages
- **Build a community** - Invite members, assign roles, create laws
- **Customize your faction** - Set colors, flags, and permissions

### Key Concepts

**Power System:**
- Every player has power that regenerates over time
- Power is lost when you die and gained when you kill others
- Faction power = sum of all member power + bonus power
- You need power to claim land (each chunk requires power)

**Territory:**
- Land is divided into chunks (16x16 block areas)
- Factions claim chunks to protect them
- Claimed land prevents other factions from building or breaking blocks
- Your faction can only claim as much land as it has power

**Relationships:**
- **Neutral** - Default relationship with other factions
- **Allied** - Friendly factions that may share permissions
- **At War** - Hostile factions in active conflict
- **Vassal/Liege** - Feudal hierarchy relationships

## Common Scenarios

### Creating Your First Faction

**Goal:** Establish your own faction and become its leader.

**Steps:**

1. **Choose a faction name** (up to 20 characters by default):
   ```
   /faction create [YourFactionName]
   /f create MyKingdom
   ```

2. **Verify creation** - You should see a confirmation message that your faction was created.

3. **Set a description** (optional but recommended):
   ```
   /f set description "A peaceful kingdom focused on building and trade"
   ```

4. **Choose your faction color**:
   ```
   /f flag set color #4169E1
   ```

5. **View your faction info**:
   ```
   /f info
   ```

**What's Next:**
- Start claiming territory near your base
- Invite friends to join your faction
- Set up roles and permissions

---

### Joining an Existing Faction

**Goal:** Become a member of an established faction.

**Methods:**

#### Method 1: Direct Invitation

1. **Receive an invitation** from a faction leader or officer:
   - The inviter uses: `/f invite YourName`
   - You'll receive a notification

2. **Accept the invitation**:
   ```
   /f join [FactionName]
   ```

3. **Verify your membership**:
   ```
   /f info
   ```

#### Method 2: Application System

1. **Find a faction you want to join**:
   ```
   /f list
   ```

2. **Submit an application**:
   ```
   /apply [FactionName]
   /apply TheKingdom
   ```

3. **To cancel your application** (if you change your mind):
   ```
   /apply [FactionName] cancel
   /apply TheKingdom cancel
   ```

4. **Wait for approval** - Faction officers will review your application.

5. **If approved**, you'll be automatically added to the faction.

#### Method 3: Force Join (Op/Admin Only)

Operators can force join any faction:
```
/f join [FactionName]
```

**After Joining:**
- Review faction laws: `/f law list`
- Check your role: `/f info`
- Ask about faction Discord or chat
- Learn faction rules and expectations

---

### Claiming Your First Territory

**Goal:** Protect your base by claiming chunks.

**Before You Start:**
- Check your faction's power: `/f power`
- Make sure you have enough power to claim (typically 1 power per chunk)
- Stand in the chunk you want to claim

**Steps:**

#### Single Chunk Claim

1. **Stand in the chunk** you want to claim (move around to ensure you're in the right spot).

2. **Claim the chunk**:
   ```
   /f claim
   ```

3. **Verify the claim**:
   ```
   /f checkclaim
   ```

#### Claiming Multiple Chunks

**Option 1: Radius Claim**

Claim multiple chunks in a square area:
```
/f claim [radius]
/f claim 2  (claims a 5x5 chunk area)
```

**Option 2: Auto-Claim**

Automatically claim chunks as you walk:
```
/f claim auto
```
- Walk through the area you want to claim
- Toggle it off when done: `/f claim auto`

**Option 3: Fill Claim**

Fill unclaimed gaps surrounded by your claims:
```
/f claim fill
```

#### Setting Your Faction Home

1. **Stand where you want your home** (must be in claimed territory):
   ```
   /f sethome
   ```

2. **Teleport to your home anytime**:
   ```
   /f home
   ```

#### Unclaiming Land

If you need to unclaim:
```
/f unclaim          (single chunk)
/f unclaim 2        (radius)
/f unclaimall       (all claims - requires confirmation)
```

**Tips:**
- Claim strategically - create a buffer zone around your important structures
- Claim contiguously if server requires it (`contiguousClaims` in config)
- Remember: you lose claims if faction power drops too low

---

### Inviting Members

**Goal:** Grow your faction by inviting trusted players.

**Prerequisites:**
- You must have permission to invite (usually leader or officer role)
- The player must be online

**Steps:**

1. **Send an invitation**:
   ```
   /f invite [PlayerName]
   /f invite Steve
   ```

2. **The player receives a notification** and can accept with:
   ```
   /f join [YourFactionName]
   ```

3. **Verify they joined**:
   ```
   /f members
   ```

4. **Assign them a role** (if needed):
   ```
   /f role set [PlayerName] [RoleName]
   /f role set Steve Member
   ```

**Managing Members:**

- **View all members**: `/f members`
- **View a player's faction**: `/f who PlayerName`
- **Remove a member**: `/f kick PlayerName`
- **Change member role**: `/f role set PlayerName NewRole`

**Best Practices:**
- Have a vetting process (interviews, trial periods)
- Clearly communicate faction rules and expectations
- Assign appropriate roles based on trust level
- Keep track of member activity
- Have a policy for inactive members

---

### Setting Up Faction Roles

**Goal:** Create a hierarchy with different permission levels.

**Default Roles:**
Most factions start with these roles:
- **Leader** - Full permissions
- **Officer** - Most permissions
- **Member** - Basic permissions

**Creating Custom Roles:**

1. **Create a new role**:
   ```
   /f role create [RoleName]
   /f role create Guard
   ```

2. **Set permissions for the role**:
   ```
   /f role setpermission [RoleName] [Permission] [true/false]
   /f role setpermission Guard claim true
   /f role setpermission Guard invite false
   /f role setpermission Guard kick false
   ```

3. **Assign players to the role**:
   ```
   /f role set [PlayerName] [RoleName]
   /f role set Alex Guard
   ```

**Common Role Structures:**

#### Basic Structure:
- Leader - Full control
- Officer - Most permissions
- Member - Basic permissions
- Recruit - Limited permissions

#### Advanced Structure:
- Leader - Full control
- Council - Strategic decisions
- General - Military operations
- Builder - Construction permissions
- Recruit - Trial members

**Useful Commands:**
- View all roles: `/f role list`
- View role details: `/f role view [RoleName]`
- Rename a role: `/f role rename [OldName] [NewName]`
- Delete a role: `/f role delete [RoleName]`
- Set default role for new members: `/f role setdefault [RoleName]`

---

### Forming an Alliance

**Goal:** Establish friendly relations with another faction.

**Steps:**

1. **Identify a potential ally** - Find a faction with compatible goals:
   ```
   /f list
   /f who [PlayerName]
   ```

2. **Contact the faction** - Coordinate through chat or messaging.

3. **Send alliance request**:
   ```
   /f ally [FactionName]
   /f ally TheKingdom
   ```

4. **The other faction accepts** by running the same command:
   ```
   /f ally [YourFactionName]
   ```

5. **Verify the alliance**:
   ```
   /f info
   ```

**After Forming an Alliance:**

1. **Configure alliance permissions** (optional):
   ```
   /f flag set alliesCanInteractWithLand true
   ```

2. **Use ally chat**:
   ```
   /f chat allies
   ```
   - Type your message
   - Toggle off to return to normal chat

3. **Coordinate strategy** - Discuss common enemies, trade, shared projects.

**Breaking an Alliance:**

If relations deteriorate:
```
/f breakalliance [FactionName]
```

**Alliance Tips:**
- Establish clear terms (defense pacts, trade agreements, etc.)
- Communicate regularly
- Help allies in wars if you have a defense pact
- Be careful about land permissions - don't allow griefing
- Document alliance terms in faction laws or external documents

---

### Declaring War

**Goal:** Enter into armed conflict with another faction.

**Before Declaring War:**

1. **Check server PVP rules** - Some servers require wars for PVP between factions.

2. **Assess the situation:**
   - Check enemy faction power: `/f power [FactionName]`
   - Review enemy claims: `/f map`
   - Coordinate with faction members and allies

3. **Prepare for war:**
   - Stockpile resources
   - Fortify defenses
   - Rally faction members
   - Secure valuables

**Declaring War:**

```
/f declarewar [EnemyFactionName]
/f declarewar TheEmpire
```

**During Wartime:**

1. **Coordinate with faction members** - Use faction chat:
   ```
   /f chat faction
   ```

2. **Invoke allies** (if you have defense pacts):
   ```
   /f invoke [AllyFaction] [EnemyFaction]
   /f invoke TheKingdom TheEmpire
   ```

3. **Strategic considerations:**
   - Reduce enemy faction power by defeating their members
   - Protect your faction power by avoiding deaths
   - If enemy power drops low enough, you may be able to claim their land
   - If enabled, destroy blocks in enemy territory during war

**Ending the War:**

**Option 1: Make Peace**

1. **Send peace request**:
   ```
   /f makepeace [EnemyFaction]
   ```

2. **Enemy faction accepts** by running the same command:
   ```
   /f makepeace [YourFaction]
   ```

**Option 2: Complete Victory**

If enemy faction is disbanded or surrenders, war ends automatically.

**Post-War:**
- Rebuild damaged structures
- Review what worked and what didn't
- Strengthen defenses for future conflicts
- Consider new alliances or strategies

---

### Establishing a Vassalage

**Goal:** Create a feudal relationship where one faction serves another.

**Understanding Vassalage:**
- **Liege** - The superior faction receiving tribute/service
- **Vassal** - The subordinate faction owing allegiance
- Vassals contribute a percentage of their power to their liege
- Creates a hierarchical structure

**Becoming a Liege (Receiving a Vassal):**

1. **Send vassalization request** to another faction:
   ```
   /f vassalize [FactionName]
   /f vassalize SmallTown
   ```

2. **The other faction accepts** using:
   ```
   /f swearfealty [YourFactionName]
   ```

3. **Verify the relationship**:
   ```
   /f info
   ```

**Becoming a Vassal (Serving a Liege):**

1. **Receive vassalization request** from another faction.

2. **Accept the request**:
   ```
   /f swearfealty [LiegeFactionName]
   /f swearfealty TheEmpire
   ```

3. **Understand your obligations:**
   - Contribute power to your liege (percentage configurable)
   - May be called to support liege in wars
   - Subject to liege's influence

**Managing Vassalage:**

**Configure land permissions**:
```
/f flag set vassalageTreeCanInteractWithLand true  (for lieges)
/f flag set liegeChainCanInteractWithLand true    (for vassals)
```

**As a Liege:**
- Grant independence to a vassal: `/f grantindependence [VassalName]`
- Coordinate with vassals using vassal chat: `/f chat vassals`

**As a Vassal:**
- Declare independence (triggers war): `/f declareindependence`

**Vassalage Tips:**
- Clearly define the terms of vassalage
- Lieges should protect and support their vassals
- Vassals should contribute military support when needed
- Consider creating multi-tier hierarchies (vassals with sub-vassals)
- Document obligations in faction laws

---

### Creating and Using Gates

**Goal:** Build a gate that can be opened and closed on command.

**Prerequisites:**
- Permission to create gates: `mf.gate` (default: true)
- Claimed territory to build in
- Materials for the gate

**Creating a Gate:**

1. **Build your gate structure:**
   - Must be at least 3 blocks high (configurable)
   - Maximum 64 blocks (configurable)
   - Cannot use restricted blocks (sand, gravel, torches, etc.)
   - Can be any shape (wall, door, portcullis, etc.)

2. **Start gate creation**:
   ```
   /gate create
   ```

3. **Select gate blocks:**
   - Right-click each block you want in the gate
   - The blocks will be highlighted/marked
   - Select all blocks that should move together

4. **Finish selection** when all blocks are selected.

5. **Verify creation** - You should receive a confirmation message.

**Using a Gate:**

- **Toggle the gate** by right-clicking any gate block
- The gate will open (blocks disappear) or close (blocks reappear)
- Only faction members can toggle gates
- Gates can be controlled with redstone (if enabled in config)

**Removing a Gate:**

1. **Start gate removal**:
   ```
   /gate remove
   ```

2. **Click on a gate block** within removal distance (default: 12 blocks).

3. **Confirm removal** - The entire gate is removed.

**Cancelling Gate Creation:**

If you make a mistake:
```
/gate cancel
```

**Gate Tips:**
- Test your gate before relying on it for defense
- Build gate houses or control rooms for security
- Use gates for main entrances, secret passages, or defensive walls
- Maximum gates per faction is configurable (default: 5)
- Gates are great for roleplay and aesthetic builds

**Common Gate Designs:**
- Castle gates (large entrance)
- Portcullis (vertical bars)
- Hidden doors (secret passages)
- Drawbridges (over moat)
- Fence gates (perimeter control)

---

### Locking Important Blocks

**Goal:** Protect valuable blocks (chests, doors, furnaces) from other faction members and outsiders.

**What Can Be Locked:**
- Chests
- Doors
- Furnaces
- Hoppers
- Brewing stands
- Most interactive blocks

**Locking Blocks:**

1. **Enable lock mode**:
   ```
   /lock
   ```

2. **Right-click the block** you want to lock.

3. **Verify the lock** - You should see a confirmation message.

4. **Exit lock mode** (optional):
   ```
   /lock cancel
   ```

**Unlocking Blocks:**

1. **Enable unlock mode**:
   ```
   /unlock
   ```

2. **Right-click the locked block** you want to unlock.

3. **Verify unlock** - You should see a confirmation message.

4. **Exit unlock mode** (optional):
   ```
   /unlock cancel
   ```

**Managing Access:**

**View who can access a locked block:**
1. Enable list mode: `/accessors list`
2. Right-click the locked block

**Grant access to someone:**
1. Enable add mode: `/accessors add`
2. Right-click the locked block
3. Right-click the player to grant access to

**Revoke access:**
1. Enable remove mode: `/accessors remove`
2. Right-click the locked block
3. Right-click the player to revoke access from

**Lock Tips:**
- Lock your personal chests to prevent faction member theft
- Lock important doors to control access
- Lock furnaces and brewing stands in shared areas
- Grant access to trusted faction members
- Use locks for role-based access control (officers only, etc.)
- Admins can force unlock blocks with `mf.force.unlock`

---

### Managing Faction Laws

**Goal:** Establish rules and regulations for your faction.

**Adding Laws:**

```
/f law add [Law Text]
/f law add "No griefing faction members"
/f law add "Help defend faction territory when online"
/f law add "Donate 10% of resources to faction storage"
```

**Viewing Laws:**

```
/f law list
```

This displays all laws with their ID numbers.

**Removing Laws:**

```
/f law remove [ID]
/f law remove 3
```

**Law System Tips:**
- Number your laws for easy reference
- Keep laws clear and concise
- Update laws as faction evolves
- Use laws for:
  - Behavioral rules
  - Resource contribution requirements
  - Conflict resolution procedures
  - Promotion criteria
  - Building guidelines
- Enforce laws consistently
- Consider creating categories (military laws, building laws, etc.)

**Example Law Sets:**

**Basic Faction:**
1. Respect all faction members
2. No stealing from faction members
3. Help defend territory when possible
4. Follow officer instructions during conflicts

**Advanced Faction:**
1. Attend mandatory faction meetings
2. Contribute 15% of resources to faction vault
3. Report enemy sightings immediately
4. Builds must follow architectural guidelines
5. Resolve disputes through faction tribunal
6. Inactive for 30 days = automatic removal

---

### Configuring Faction Settings

**Goal:** Customize your faction's behavior and appearance.

**Basic Settings:**

**Change Faction Name:**
```
/f set name [NewName]
/f set name "The Grand Empire"
```

**Set Description:**
```
/f set description [Description]
/f set description "A peaceful trading faction"
```

**Set Prefix:**
```
/f set prefix [TAG]
/f set prefix [GE]
```

**Faction Flags:**

View all available flags and their values:
```
/f flag list
```

Set a flag:
```
/f flag set [FlagName] [Value]
```

**Common Flag Configurations:**

**Peaceful/Trade Faction:**
```
/f flag set neutral true
/f flag set color #32CD32
/f flag set alliesCanInteractWithLand true
/f flag set protectVillagerTrade false
```

**Military Faction:**
```
/f flag set neutral false
/f flag set allowFriendlyFire true
/f flag set color #8B0000
/f flag set alliesCanInteractWithLand false
```

**Protective/Private Faction:**
```
/f flag set neutral false
/f flag set alliesCanInteractWithLand false
/f flag set enableMobProtection true
/f flag set protectVillagerTrade true
/f flag set acceptBonusPower false
```

See [FACTION_FLAGS.md](FACTION_FLAGS.md) for complete flag documentation.

---

## Advanced Features

### Faction Chat Channels

Toggle between chat modes:
```
/f chat faction  - Talk only to faction members
/f chat vassals  - Talk to vassals and liege
/f chat allies   - Talk to allied factions
```

Toggle back to normal chat by running the command again.

### Power Management

Understanding power is crucial:
- **View your power**: `/f power`
- **View faction power**: `/f power [FactionName]`
- Power regenerates over time (configurable)
- Power lost on death, gained on kills
- Total faction power = sum of member power + bonus power

### Mapping and Territory

**View Territory Map:**
```
/f map          - Normal view
/f map normal   - Normal view
/f map diplomatic - Shows relationships
```

The map uses characters to represent:
- Your faction
- Allied factions
- Enemy factions
- Neutral factions
- Wilderness

### Dueling System

Challenge players to honor duels:
```
/duel challenge [PlayerName]  - Send challenge
/duel accept [PlayerName]     - Accept challenge
/duel cancel [PlayerName]     - Decline/cancel
```

Duels are time-limited and notify nearby players.

### Applications System

**For Players:**
- Apply to factions: `/apply [FactionName]`
- Cancel application: `/apply [FactionName] cancel`

**For Faction Officers:**
- View applications: `/showapps`
- Approve application: `/approveapp [PlayerName]`
- Deny application: `/denyapp [PlayerName]`

---

## Tips and Best Practices

### Starting Out

1. **Start small** - Don't claim too much land initially
2. **Build in claimed territory** - Protect your base
3. **Join or create carefully** - Research factions before committing
4. **Learn the commands** - Use `/f help` frequently
5. **Understand power** - It's the key resource

### Growing Your Faction

1. **Recruit strategically** - Quality over quantity
2. **Establish clear rules** - Use the law system
3. **Create a hierarchy** - Assign meaningful roles
4. **Claim efficiently** - Use claim fill and auto-claim
5. **Build community** - Use faction chat, Discord, etc.

### Diplomacy

1. **Communicate clearly** - Document agreements
2. **Honor commitments** - Build reputation
3. **Be strategic** - Form alliances that benefit your goals
4. **Stay informed** - Use `/f list` and `/f who` regularly
5. **Prepare for conflict** - Even in peace, be ready for war

### Defense

1. **Fortify borders** - Build walls and defenses
2. **Use gates wisely** - Control entry points
3. **Lock valuables** - Protect important items
4. **Keep power high** - Encourage member activity
5. **Watch for spies** - Be careful who you trust

### Resource Management

1. **Build communal storage** - Shared resources help everyone
2. **Lock personal chests** - Prevent internal theft
3. **Contribute to faction** - Everyone benefits from cooperation
4. **Track faction bank** - If using economy plugins
5. **Plan big projects** - Coordinate resource gathering

---

## Troubleshooting

### Common Issues

**"You don't have enough power to claim this chunk"**
- **Solution**: Increase faction power by adding members or waiting for power regeneration
- Check current power: `/f power`

**"This land is already claimed"**
- **Solution**: Use `/f checkclaim` to see who owns it
- Try claiming adjacent unclaimed land
- View map: `/f map`

**"You don't have permission to do that"**
- **Solution**: Check your role permissions
- Ask a faction officer or leader to adjust your role: `/f role set`

**"You are not in a faction"**
- **Solution**: Create a faction (`/f create`) or join one (`/f join`)
- Check if you were kicked: `/f who YourName`

**Can't teleport to faction home**
- **Solution**: Make sure you're not moving during the delay
- Check that faction home is set: `/f sethome` in claimed territory
- Ensure you have permission to use `/f home`

**Gate not working**
- **Solution**: Verify gate was created properly
- Check you're within interaction range
- Ensure blocks aren't obstructed
- Verify you're a faction member

**Lock not working**
- **Solution**: Make sure you're the lock owner or have access
- Use `/accessors list` to check access list
- Try unlocking and re-locking: `/unlock` then `/lock`

### Getting Help

1. **In-game help**: `/f help`
2. **Server staff**: Ask moderators or admins
3. **Documentation**: Read [COMMANDS.md](COMMANDS.md), [CONFIG.md](CONFIG.md), [FACTION_FLAGS.md](FACTION_FLAGS.md)
4. **Support Discord**: Check README.md for Discord link
5. **Bug reports**: Submit issues on GitHub

---

## Additional Resources

- [Commands Reference](COMMANDS.md) - Complete list of all commands
- [Configuration Guide](CONFIG.md) - Server configuration options
- [Faction Flags](FACTION_FLAGS.md) - Detailed flag documentation
- [GitHub Repository](https://github.com/Dans-Plugins/Medieval-Factions) - Source code and issues
- [SpigotMC Page](https://www.spigotmc.org/resources/medieval-factions-sovereign-nation-simulator.79941/) - Plugin downloads and updates

---

**Welcome to Medieval Factions! Build your empire, forge alliances, and create your own medieval story!**
