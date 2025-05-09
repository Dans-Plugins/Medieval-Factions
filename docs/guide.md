# **Medieval Factions Guide​**
### **Viewing Faction Info**
* In order to view what factions exist, type /mf list.
* To see more information about any of the factions, type /mf info (faction-name).
* To know what faction someone is in, type /mf who (player-name).

### **Joining a Faction**
* If you're not in a faction, you won't be able to protect your things from others, so it's best to know how to join one.
* Before you join a faction, you have to be invited. If someone invites you, you will be alerted on the screen and will need to type /mf join (faction-name) to accept.

### **Creating a Faction**
* If you would rather lead a civilization rather than join one, type /mf create (faction-name).
* To invite others to your faction, type /mf invite (player-name).
* To set the description of your faction, type /mf desc (description).
* To view the laws of your faction, type /mf laws.
* To add a law, type /mf addlaw (law) or /mf al (law).
* To remove a law, type /mf (removelaw (number) or /mf rl (number).
* To edit a law, type /mf editlaw (number) (edited law) or /mf el (number) (edited law).

### **Claiming Land**
* Currently only owners and officers of factions can claim land, and only owners can toggle the autoclaim flag.
* To see if land is claimed, type /mf checkclaim or /mf cc.
* To claim land, type /mf claim.
* To unclaim land, type /mf unclaim.
* To enable autoclaim, type /mf autoclaim or /mf ac.
* To unclaim all land, type /mf unclaimall or /mf ua.

### **Warfare**
* Players can only hurt eachother when they're at war or factionless. Only owners and officers of factions can declare war or make peace.
* To view who you are at war with, type /mf info.
* To declare war on a faction, type /mf declarewar (faction) or /mf dw (faction).
* To offer peace to a faction, type /mf makepeace (faction) or /mf mp (faction). Both factions will need to make peace before peace can be had.
* To conquer someone's land, type /mf claim in their territory. This is only possible if you are at war and if they have more land than power.

### **Alliances**
* Factions can be allied with one another. Allies can't declare war on each other. When someone declares war on a faction, all of the allies of that factions declare war back.
* To view who you are allied with, type /mf info.
* To request an alliance with another faction, type /mf ally (faction). Both factions must request an alliance before an alliance is made.
* To break an alliance with another faction, type /mf breakalliance (faction).

### **Locks**
* Locks only work on claimed land, so if land is unclaimed or conquered, locks will also be removed.
* You can only lock chests or doors right now.
* To lock something, type /mf lock and right click it. Type /mf lock cancel to cancel this.
* To unlock something, type /mf unlock and right click it. Type /mf unlock cancel to cancel this.
* To check who has access to something, type /mf checkaccess or /mf cc and right click it. Type /mf checkaccess cancel or /mf cc cancel to cancel this.
* To grant access to something for someone, type /mf grantaccess (player) or /mf ga (player) and right click it. Type /mf grantaccess cancel or /mf ga cancel to cancel this.
* To revoke access to something for someone, type /mf revokeaccess (player) or /mf ra (player) and right click it. Type /mf revokeaccess cancel or /mf ra cancel to cancel this.

### **Vassals**
* Factions can vassalize other factions.
* When one faction agrees to be another's vassal, they are essentially swearing loyalty.
* If a faction has one or more vassals, they are considered a liege.
* Lieges can be vassals as well.
* Lieges are granted bonus power proportional to their vassals' power. If a liege drops below 50% of its maximum cumulative power, it will no longer receive their vassal bonuses.
* Vassals cannot be declared war on by any factions except those with the same liege.

### Gates
You can create a gate using the /mf gate create command. You need to be holding a golden hoe. The command will guide you through the process.

If you’re stuck, check out this example [here](https://imgur.com/a/6A5CvHz).