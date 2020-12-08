package factionsystem.Subsystems;

import factionsystem.MedievalFactions;
import factionsystem.Objects.ClaimedChunk;
import factionsystem.Objects.Duel;
import factionsystem.Objects.Faction;
import factionsystem.Objects.Gate;
import factionsystem.Objects.LockedBlock;
import factionsystem.Objects.PlayerActivityRecord;
import factionsystem.Objects.PlayerPowerRecord;
import factionsystem.Util.Pair;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import java.io.File;
import java.time.ZonedDateTime;
import java.util.*;

import static org.bukkit.Bukkit.*;

public class UtilitySubsystem {

    // non-static methods
    
    private ClaimedChunk isChunkClaimed(double x, double y, String world)
    {
    	for (ClaimedChunk chunk : MedievalFactions.getInstance().claimedChunks)
    	{
    		if (x == chunk.getCoordinates()[0] && y == chunk.getCoordinates()[1] && world.equalsIgnoreCase(chunk.getWorld()))
    		{
    			return chunk;
    		}
    	}
    	
    	return null;
    }
    
    public void addChunkAtPlayerLocation(Player player) {
        double[] playerCoords = new double[2];
        playerCoords[0] = player.getLocation().getChunk().getX();
        playerCoords[1] = player.getLocation().getChunk().getZ();
        for (Faction faction : MedievalFactions.getInstance().factions) {
            if (faction.isOwner(player.getUniqueId()) || faction.isOfficer(player.getUniqueId())) {

                // check if land is already claimed
                ClaimedChunk chunk = isChunkClaimed(playerCoords[0], playerCoords[1], player.getLocation().getWorld().getName());
                if (chunk != null)
        		{
                    if (playerCoords[0] == chunk.getCoordinates()[0] && playerCoords[1] == chunk.getCoordinates()[1]) {

                        // if holder is player's faction
                        if (chunk.getHolder().equalsIgnoreCase(faction.getName()) && !getPlayersFaction(player.getUniqueId(), MedievalFactions.getInstance().factions).getAutoClaimStatus()) {
                            player.sendMessage(ChatColor.RED + "This land is already claimed by your faction!");
                            return;
                        }
                        else {

                            // check if faction has more land than their demesne limit
                            for (Faction targetFaction : MedievalFactions.getInstance().factions) {
                                if (chunk.getHolder().equalsIgnoreCase(targetFaction.getName())) {
                                    if (targetFaction.getCumulativePowerLevel() < getChunksClaimedByFaction(targetFaction.getName(), MedievalFactions.getInstance().claimedChunks)) {

                                        // is at war with target faction
                                        if (faction.isEnemy(targetFaction.getName()) || everyPlayerInFactionExperiencingPowerDecay(targetFaction)) {

                                            if (MedievalFactions.getInstance().getConfig().getBoolean("surroundedChunksProtected")) {
                                                if (isClaimedChunkSurroundedByChunksClaimedBySameFaction(chunk)) {
                                                    player.sendMessage(ChatColor.RED + "Target faction has claimed the chunks to the north, east, south and west of this chunk! It cannot be conquered!");
                                                    return;
                                                }
                                            }

                                            // CONQUERABLE

                                            // remove locks on this chunk
                                            Iterator<LockedBlock> itr = MedievalFactions.getInstance().lockedBlocks.iterator();
                                            while (itr.hasNext()) {
                                                LockedBlock block = itr.next();
                                                if (chunk.getChunk().getWorld().getBlockAt(block.getX(), block.getY(), block.getZ()).getChunk().getX() == chunk.getChunk().getX() &&
                                                        chunk.getChunk().getWorld().getBlockAt(block.getX(), block.getY(), block.getZ()).getChunk().getZ() == chunk.getChunk().getZ()) {
                                                    itr.remove();
                                                }
                                            }

                                            MedievalFactions.getInstance().claimedChunks.remove(chunk);

                                            ClaimedChunk newChunk = new ClaimedChunk(player.getLocation().getChunk());
                                            newChunk.setHolder(faction.getName());
                                            newChunk.setWorld(player.getLocation().getWorld().getName());
                                            MedievalFactions.getInstance().claimedChunks.add(newChunk);
                                            player.sendMessage(ChatColor.GREEN + "Land conquered from " + targetFaction.getName() + "! Demesne Size: " + getChunksClaimedByFaction(faction.getName(), MedievalFactions.getInstance().claimedChunks) + "/" + faction.getCumulativePowerLevel());

                                            sendAllPlayersInFactionMessage(targetFaction, ChatColor.RED + getPlayersFaction(player.getUniqueId(), MedievalFactions.getInstance().factions).getName() + " has conquered land from your faction!");

                                            return;
                                        }
                                        else {
                                            if (MedievalFactions.getInstance().getConfig().getBoolean("powerDecreases")) {
                                                player.sendMessage(ChatColor.RED + "In order for you to conquer land, this faction must be at war with you or every member must be experiencing power decay.");
                                            } else {
                                                player.sendMessage(ChatColor.RED + "Your factions have to be at war in order for you to conquer land.");
                                            }
                                            return;
                                        }
                                    }
                                }
                            }

                            if (!getPlayersFaction(player.getUniqueId(), MedievalFactions.getInstance().factions).getAutoClaimStatus()) {
                                player.sendMessage(ChatColor.RED + "This land is already claimed by " + chunk.getHolder());
                            }

                            return;
                        }
                    }
                }

                ClaimedChunk newChunk = new ClaimedChunk(player.getLocation().getChunk());
                newChunk.setHolder(faction.getName());
                newChunk.setWorld(player.getLocation().getWorld().getName());
                MedievalFactions.getInstance().claimedChunks.add(newChunk);
                player.sendMessage(ChatColor.GREEN + "Land claimed! Demesne Size: " + getChunksClaimedByFaction(faction.getName(), MedievalFactions.getInstance().claimedChunks) + "/" + faction.getCumulativePowerLevel());
                return;
            }
        }
    }

    private boolean everyPlayerInFactionExperiencingPowerDecay(Faction faction) {
        int numExperiencingPowerDecay = 0;
        for (UUID uuid : faction.getMemberArrayList()) {
            PlayerActivityRecord record = getPlayerActivityRecord(uuid, MedievalFactions.getInstance().playerActivityRecords);
            if (!getPlayer(uuid).isOnline() && MedievalFactions.getInstance().getConfig().getBoolean("powerDecreases")
                    && record.getMinutesSinceLastLogout() > MedievalFactions.getInstance().getConfig().getInt("minutesBeforePowerDecrease")) {
                numExperiencingPowerDecay++;
            }
        }
        return (numExperiencingPowerDecay == faction.getMemberArrayList().size());
    }

    public void removeChunkAtPlayerLocation(Player player) {
        double[] playerCoords = new double[2];
        playerCoords[0] = player.getLocation().getChunk().getX();
        playerCoords[1] = player.getLocation().getChunk().getZ();

        if (MedievalFactions.getInstance().adminsBypassingProtections.contains(player.getUniqueId())) {
        	ClaimedChunk chunk = isChunkClaimed(playerCoords[0], playerCoords[1], player.getLocation().getWorld().getName());
            if (chunk != null)
            {
                removeChunk(chunk, player, getFaction(chunk.getHolder(), MedievalFactions.getInstance().factions));
                player.sendMessage(ChatColor.GREEN + "Land unclaimed using admin bypass!");
                return;
            }
            player.sendMessage(ChatColor.RED + "This land is not currently claimed!");
            return;
        }

        for (Faction faction : MedievalFactions.getInstance().factions) {
            if (faction.isOwner(player.getUniqueId()) || faction.isOfficer(player.getUniqueId())) {
                // check if land is claimed by player's faction
            	ClaimedChunk chunk = isChunkClaimed(playerCoords[0], playerCoords[1], player.getLocation().getWorld().getName());
            	if (chunk != null)
            	{
                    // if holder is player's faction
                    if (chunk.getHolder().equalsIgnoreCase(faction.getName())) {
                        removeChunk(chunk, player, faction);
                        player.sendMessage(ChatColor.GREEN + "Land unclaimed.");

                        return;
                    }
                    else {
                        player.sendMessage(ChatColor.RED + "This land is claimed by " + chunk.getHolder());
                        return;
                    }
                }

            }
        }
    }

    public void removeChunk(ClaimedChunk chunk, Player player, Faction faction) {
        String identifier = (int)chunk.getChunk().getX() + "_" + (int)chunk.getChunk().getZ();

        // if faction home is located on this chunk
        Location factionHome = getPlayersFaction(player.getUniqueId(), MedievalFactions.getInstance().factions).getFactionHome();
        if (factionHome != null) {
            if (factionHome.getChunk().getX() == chunk.getChunk().getX() && factionHome.getChunk().getZ() == chunk.getChunk().getZ()
            		&& chunk.getWorld().equalsIgnoreCase(player.getLocation().getWorld().getName())) {
                // remove faction home
                faction.setFactionHome(null);
                sendAllPlayersInFactionMessage(faction, ChatColor.RED + "Your faction home has been removed!");

            }
        }

        // remove locks on this chunk
        Iterator<LockedBlock> itr = MedievalFactions.getInstance().lockedBlocks.iterator();
        while (itr.hasNext()) {
            LockedBlock block = itr.next();
            if (chunk.getChunk().getWorld().getBlockAt(block.getX(), block.getY(), block.getZ()).getChunk().getX() == chunk.getChunk().getX() &&
                    chunk.getChunk().getWorld().getBlockAt(block.getX(), block.getY(), block.getZ()).getChunk().getZ() == chunk.getChunk().getZ() &&
                    block.getWorld().equalsIgnoreCase(chunk.getWorld())) {
                itr.remove();
            }
        }
        
        // remove any gates in this chunk
        Iterator<Gate> gtr = faction.getGates().iterator();
        while(gtr.hasNext())
        {
        	Gate gate = gtr.next();
        	if (isGateInChunk(gate, chunk))
        	{
//        		System.out.println("Removing gate " + gate.getName());
        		faction.removeGate(gate);
        		gtr.remove();
        	}
        }

        MedievalFactions.getInstance().claimedChunks.remove(chunk);
    }
    
    public String checkOwnershipAtPlayerLocation(Player player) {
        double[] playerCoords = new double[2];
        playerCoords[0] = player.getLocation().getChunk().getX();
        playerCoords[1] = player.getLocation().getChunk().getZ();
        ClaimedChunk chunk = isChunkClaimed(playerCoords[0], playerCoords[1], player.getLocation().getWorld().getName());
        if (chunk != null)
        {
            return chunk.getHolder();
        }
        return "unclaimed";
    }
    
    public boolean isGateInChunk(Gate gate, ClaimedChunk chunk)
    {
    	if ((gate.getTopLeftChunkX() == chunk.getCoordinates()[0] || gate.getBottomRightChunkX() == chunk.getCoordinates()[0])
			&& (gate.getTopLeftChunkZ() == chunk.getCoordinates()[1] || gate.getBottomRightChunkZ() == chunk.getCoordinates()[1]))
    	{
    		return true;
    	}
    	return false;
    }
    
    public boolean isChest(Block block) {
        return block.getType() == Material.CHEST;
    }

    public static PlayerActivityRecord getPlayerActivityRecord(UUID uuid, ArrayList<PlayerActivityRecord> playerActivityRecords)
    {
    	for (PlayerActivityRecord record : playerActivityRecords)
    	{
    		if (record.getPlayerUUID().equals(uuid))
    		{
    			return record;
    		}
    	}
    	return null;
    }

    public void resetPowerRecords() {
        // reset individual records
        System.out.println("Resetting individual power records.");
        for (PlayerPowerRecord record : MedievalFactions.getInstance().playerPowerRecords) {
            record.setPowerLevel(MedievalFactions.getInstance().getConfig().getInt("initialPowerLevel"));
        }
    }

    public boolean isBlockLocked(Block block) {
        return isBlockLocked(block.getX(), block.getY(), block.getZ(), block.getWorld().getName());
    }

    public boolean isBlockLocked(int x, int y, int z, String world) {
        for (LockedBlock block : MedievalFactions.getInstance().lockedBlocks) {
            if (block.getX() == x && block.getY() == y && block.getZ() == z && block.getWorld().equalsIgnoreCase(world)) {
                return true;
            }
        }
        return false;
    }

    public LockedBlock getLockedBlock(Block block) {
        return getLockedBlock(block.getX(), block.getY(), block.getZ(), block.getWorld().getName());
    }

    public LockedBlock getLockedBlock(int x, int y, int z, String world) {
        for (LockedBlock block : MedievalFactions.getInstance().lockedBlocks) {
            if (block.getX() == x && block.getY() == y && block.getZ() == z && block.getWorld().equalsIgnoreCase(world)) {
                return block;
            }
        }
        return null;
    }
    
    public boolean isGateBlock(Block targetBlock)
    {
    	for (Faction faction : MedievalFactions.getInstance().factions)
    	{
    		for (Gate gate : faction.getGates())
    		{
    			if (gate.hasBlock(targetBlock))
    			{
    				return true;
    			}
    		}
    	}
    	return false;
    }

    // static methods ----------------------------

    public static Duel getDuel(Player player, Player target)
    {
    	for (Duel duel : MedievalFactions.getInstance().duelingPlayers)
    	{
    		if (duel.hasPlayer(player) && duel.hasPlayer(target))
    		{
    			return duel;
    		}
    	}
    	return null;
    }

    public static Duel getDuel(Player player)
    {
    	for (Duel duel : MedievalFactions.getInstance().duelingPlayers)
    	{
    		if (duel.isChallenged(player) || duel.isChallenger(player))
    		{
    			return duel;
    		}
    	}
    	return null;
    }
    
    public static Faction getGateFaction(Gate gate, ArrayList<Faction> factions)
    {
    	for (Faction faction : factions)
    	{
    		if (faction.getGates().contains(gate))
    		{
    			return faction;
    		}
    	}
    	return null;
    }
    
    public static Gate getGate(Block targetBlock, ArrayList<Faction> factions)
    {
    	for (Faction faction : factions)
    	{
    		for (Gate gate : faction.getGates())
    		{
    			if (gate.hasBlock(targetBlock))
    			{
    				return gate;
    			}
    		}
    	}
    	return null;
    }
    
    public static void startCreatingGate(Player player, String name)
    {
    	if (!MedievalFactions.getInstance().creatingGatePlayers.containsKey(player.getUniqueId()))
    	{
    		Gate gate = new Gate();
    		gate.setName(name);
    		MedievalFactions.getInstance().creatingGatePlayers.put(player.getUniqueId(), gate);
    	}
    	else
    	{
    		System.out.println("WARNING: Player has already started creating the gate. startCreatingGate() call ignored.");
    	}
    }
    
    public static boolean isGateBlock(Block targetBlock, ArrayList<Faction> factions)
    {
    	for (Faction faction : factions)
    	{
    		for (Gate gate : faction.getGates())
    		{
    			if (gate.hasBlock(targetBlock))
    			{
    				return true;
    			}
    		}
    	}
    	return false;
    }

    public static boolean isFactionExceedingTheirDemesneLimit(Faction faction, ArrayList<ClaimedChunk> claimedChunks) {
        return (getChunksClaimedByFaction(faction.getName(), claimedChunks) > faction.getCumulativePowerLevel());
    }
    
    public static void informPlayerIfTheirLandIsInDanger(Player player, ArrayList<Faction> factions, ArrayList<ClaimedChunk> claimedChunks) {
        Faction faction = getPlayersFaction(player.getUniqueId(), factions);
        if (faction != null) {
            if (isFactionExceedingTheirDemesneLimit(faction, claimedChunks)) {
                player.sendMessage(ChatColor.RED + "Your faction has more claimed chunks than power! Your land can be conquered!");
            }
        }
    }

    public static void removeLock(Block block, ArrayList<LockedBlock> lockedBlocks) {
        for (LockedBlock b : lockedBlocks) {
            if (b.getX() == block.getX() && b.getY() == block.getY() && b.getZ() == block.getZ() && block.getWorld().getName().equalsIgnoreCase(b.getWorld())) {
                lockedBlocks.remove(b);
                return;
            }
        }
    }

    public static LockedBlock getLockedBlock(Block block, ArrayList<LockedBlock> lockedBlocks) {
        return getLockedBlock(block.getX(), block.getY(), block.getZ(), block.getWorld().getName(), lockedBlocks);
    }

    public static LockedBlock getLockedBlock(int x, int y, int z, String world, ArrayList<LockedBlock> lockedBlocks) {
        for (LockedBlock block : lockedBlocks) {
            if (block.getX() == x && block.getY() == y && block.getZ() == z && block.getWorld().equalsIgnoreCase(world)) {
                return block;
            }
        }
        return null;
    }    

    public static boolean isBlockLocked(Block block, ArrayList<LockedBlock> lockedBlocks) {
        return isBlockLocked(block.getX(), block.getY(), block.getZ(), block.getWorld().getName(), lockedBlocks);
    }

    public static boolean isBlockLocked(int x, int y, int z, String world, ArrayList<LockedBlock> lockedBlocks) {
        for (LockedBlock block : lockedBlocks) {
            if (block.getX() == x && block.getY() == y && block.getZ() == z && block.getWorld().equalsIgnoreCase(world)) {
                return true;
            }
        }
        return false;
    }
   
    public static boolean isInFaction(UUID playerUUID, ArrayList<Faction> factions) {
        // membership check
        for (Faction faction : factions) {
            if (faction.isMember(playerUUID)) {
                return true;
            }
        }
        return false;
    }

    public static Faction getPlayersFaction(UUID playerUUID, ArrayList<Faction> factions) {
        // membership check
        for (Faction faction : factions) {
            if (faction.isMember(playerUUID)) {
                return faction;
            }
        }
        return null;
    }

    public static void sendFactionInfo(Player player, Faction faction, int power) {
        player.sendMessage(ChatColor.BOLD + "" + ChatColor.AQUA + faction.getName() + " Faction Info" + "\n----------\n");
        player.sendMessage(ChatColor.AQUA + "Name: " + faction.getName() + "\n");
        player.sendMessage(ChatColor.AQUA + "Owner: " + findPlayerNameBasedOnUUID(faction.getOwner()) + "\n");
        player.sendMessage(ChatColor.AQUA + "Description: " + faction.getDescription() + "\n");
        player.sendMessage(ChatColor.AQUA + "Population: " + faction.getMemberList().size() + "\n");
        if (faction.hasLiege()) {
            player.sendMessage(ChatColor.AQUA + "Liege: " + faction.getLiege() + "\n");
        }
        if (faction.isLiege()) {
            player.sendMessage(ChatColor.AQUA + "Vassals: " + faction.getVassalsSeparatedByCommas() + "\n");
        }
        player.sendMessage(ChatColor.AQUA + "Allied With: " + faction.getAlliesSeparatedByCommas() + "\n");
        player.sendMessage(ChatColor.AQUA + "At War With: " + faction.getEnemiesSeparatedByCommas() + "\n");
        player.sendMessage(ChatColor.AQUA + "Power Level: " + faction.getCumulativePowerLevel() + "\n");
        player.sendMessage(ChatColor.AQUA + "Demesne Size: " + power + "/" + faction.getCumulativePowerLevel() + "\n");
        player.sendMessage(ChatColor.AQUA + "----------\n");
    }

    public static void sendFactionMembers(Player player, Faction faction) {
        ArrayList<UUID> members = faction.getMemberList();
        player.sendMessage(ChatColor.BOLD + "" + ChatColor.AQUA + "Members of " + faction.getName() + "\n----------\n");
        for (UUID member : members) {
            // Is Owner
            if (member.equals(faction.getOwner())){
                player.sendMessage(ChatColor.AQUA + findPlayerNameBasedOnUUID(member) + "**\n");
            } else if (faction.isOfficer(member)) {
                player.sendMessage(ChatColor.AQUA + findPlayerNameBasedOnUUID(member) + "*\n");
            } else {
                player.sendMessage(ChatColor.AQUA + findPlayerNameBasedOnUUID(member) + "\n");
            }
        }
        player.sendMessage(ChatColor.AQUA + "----------\n");
    }

    public static String createStringFromFirstArgOnwards(String[] args) {
        StringBuilder name = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            name.append(args[i]);
            if (!(i == args.length - 1)) {
                name.append(" ");
            }
        }
        return name.toString();
    }

    public static void removeAllClaimedChunks(String factionName, ArrayList<ClaimedChunk> claimedChunks) {

        Iterator<ClaimedChunk> itr = claimedChunks.iterator();

        while (itr.hasNext()) {
            ClaimedChunk currentChunk = itr.next();
            if (currentChunk.getHolder().equalsIgnoreCase(factionName)) {

                String identifier = (int) currentChunk.getChunk().getX() + "_" + (int) currentChunk.getChunk().getZ();

                try {

                    // delete file associated with chunk
                    System.out.println("Attempting to delete file plugins plugins/MedievalFactions/claimedchunks/" + identifier + ".txt");
                    File fileToDelete = new File("plugins/MedievalFactions/claimedchunks/" + identifier + ".txt");
                    if (fileToDelete.delete()) {
                        System.out.println("Success. File deleted.");
                    } else {
                        System.out.println("There was a problem deleting the file.");
                    }

                    itr.remove();
                }
                catch(Exception e) {
                    System.out.println("An error has occurred during claimed chunk removal.");
                }
            }
        }
    }

    public static void sendAllPlayersInFactionMessage(Faction faction, String message) {
        ArrayList<UUID> members = faction.getMemberArrayList();
        for (UUID member : members) {
            try {
                Player target = getServer().getPlayer(member);
                target.sendMessage(message);
            }
            catch(Exception ignored) {

            }
        }
    }

    public static int getChunksClaimedByFaction(String factionName, ArrayList<ClaimedChunk> claimedChunks) {
        int counter = 0;
        for (ClaimedChunk chunk : claimedChunks) {
            if (chunk.getHolder().equalsIgnoreCase(factionName)) {
                counter++;
            }
        }
        return counter;
    }

    public static PlayerPowerRecord getPlayersPowerRecord(UUID playerUUID, ArrayList<PlayerPowerRecord> powerRecords ) {
        for (PlayerPowerRecord record : powerRecords) {
            if (record.getPlayerUUID().equals(playerUUID)) {
                return record;
            }
        }
        return null;
    }

    public static Faction getFaction(String name, ArrayList<Faction> factions) {
        for (Faction faction : factions) {
            if (faction.getName().equalsIgnoreCase(name)) {
                return faction;
            }
        }
        return null;
    }

    public static boolean isPlayerAFactionOwner(UUID player, ArrayList<Faction> factions){
        if (isInFaction(player, factions)){
            Faction faction = getPlayersFaction(player, factions);
            return faction.getOwner().equals(player);
        } else {
            return false;
        }
    }

    public static boolean isPlayerAFactionOfficer(UUID player, ArrayList<Faction> factions) {
        if (isInFaction(player, factions)){
            Faction faction = getPlayersFaction(player, factions);
            return faction.isOfficer(player);
        } else {
            return false;
        }
    }

    public static void invokeAlliances(String victimFactionName, String declaringFactionName, ArrayList<Faction> factions) {
        Faction victimFaction = getFaction(victimFactionName, factions);
        Faction declaringFaction = getFaction(declaringFactionName, factions);

        if (victimFaction != null && declaringFaction != null)  {
            for (String alliedFaction : victimFaction.getAllies()) {
                if (!(getFaction(alliedFaction, factions).isEnemy(declaringFactionName)) && !(declaringFaction.isEnemy(alliedFaction))) {
                    // add enemies
                    getFaction(alliedFaction, factions).addEnemy(declaringFactionName);
                    declaringFaction.addEnemy(alliedFaction);

                    // inform parties
                    sendAllPlayersInFactionMessage(victimFaction, ChatColor.GREEN + "Your ally " + alliedFaction + " has joined you in war!");
                    sendAllPlayersInFactionMessage(getFaction(alliedFaction, factions), ChatColor.RED + "Your ally " + victimFactionName + " has called you into war with " + declaringFactionName + "!");
                    sendAllPlayersInFactionMessage(declaringFaction, ChatColor.RED  + alliedFaction + " has joined the war on your enemy's side!");

                }
            }
        }

    }
    
    public static boolean isClaimed(Chunk chunk, ArrayList<ClaimedChunk> claimedChunks) {

        for (ClaimedChunk claimedChunk : claimedChunks) {
            if (claimedChunk.getCoordinates()[0] == chunk.getX() && claimedChunk.getCoordinates()[1] == chunk.getZ() && claimedChunk.getWorld().equalsIgnoreCase(chunk.getWorld().getName())) {
                return true;
            }
        }
        return false;
    }

    public static ClaimedChunk getClaimedChunk(int x, int z, String world, ArrayList<ClaimedChunk> claimedChunks) {
        for (ClaimedChunk claimedChunk : claimedChunks) {
            if (claimedChunk.getCoordinates()[0] == x && claimedChunk.getCoordinates()[1] == z && claimedChunk.getWorld().equalsIgnoreCase(world)) {
                return claimedChunk;
            }
        }
        return null;
    }

    public static void removeAllLocks(String factionName, ArrayList<LockedBlock> lockedBlocks) {
        Iterator<LockedBlock> itr = lockedBlocks.iterator();

        while (itr.hasNext()) {
            LockedBlock currentBlock = itr.next();
            if (currentBlock.getFactionName().equalsIgnoreCase(factionName)) {

                String identifier = currentBlock.getX() + "_" + currentBlock.getY() + "_" + currentBlock.getZ();

                try {

                    // delete file associated with chunk
                    System.out.println("Attempting to delete file plugins/MedievalFactions/lockedblocks/" + identifier + ".txt");
                    File fileToDelete = new File("plugins/Medievalfactions/lockedblocks/" + identifier + ".txt");
                    if (fileToDelete.delete()) {
                        System.out.println("Success. File deleted.");
                    } else {
                        System.out.println("There was a problem deleting the file.");
                    }

                    itr.remove();
                }
                catch(Exception e) {
                    System.out.println("An error has occurred during lock removal.");
                }
            }
        }
    }

    public void ensureSmoothTransitionBetweenVersions() {
        // this piece of code is to ensure that saves don't become broken when updating to v3.2 from a previous version
        File saveFolder = new File("./plugins/medievalfactions/");
        if (saveFolder.exists()) { // TODO: fix this so that it doesn't run every time
//            System.out.println("[ALERT] Old save folder name (pre v3.2) detected. Updating for compatibility.");

            // rename directory
            File newSaveFolder = new File("./plugins/MedievalFactions/");
            saveFolder.renameTo(newSaveFolder);
        }

        // this piece of code is to fix config values not matching when updating to v3.3 (after v3.3 there is version mismatch handling)
        if (!MedievalFactions.getInstance().getConfig().isSet("version")) {
            System.out.println("Config.yml doesn't have version entry!");
            MedievalFactions.getInstance().config.handleVersionMismatch();
        }
    }

    public boolean arePlayersFactionsNotEnemies(Player player1, Player player2) {
        Pair<Integer, Integer> factionIndices = getFactionIndices(player1, player2);
        int attackersFactionIndex = factionIndices.getLeft();
        int victimsFactionIndex = factionIndices.getRight();

        return !(MedievalFactions.getInstance().factions.get(attackersFactionIndex).isEnemy(MedievalFactions.getInstance().factions.get(victimsFactionIndex).getName())) &&
                !(MedievalFactions.getInstance().factions.get(victimsFactionIndex).isEnemy(MedievalFactions.getInstance().factions.get(attackersFactionIndex).getName()));
    }

    public boolean arePlayersInSameFaction(Player player1, Player player2) {
        Pair<Integer, Integer> factionIndices = getFactionIndices(player1, player2);
        int attackersFactionIndex = factionIndices.getLeft();
        int victimsFactionIndex = factionIndices.getRight();

        // if attacker and victim are both in a faction
        if (arePlayersInAFaction(player1, player2)){
            // if attacker and victim are part of the same faction
            return attackersFactionIndex == victimsFactionIndex;
        } else {
            return false;
        }
    }

    public boolean arePlayersInAFaction(Player player1, Player player2) {
        return isInFaction(player1.getUniqueId(), MedievalFactions.getInstance().factions) && isInFaction(player2.getUniqueId(), MedievalFactions.getInstance().factions);
    }

    public Pair<Integer, Integer> getFactionIndices(Player player1, Player player2){
        int attackersFactionIndex = 0;
        int victimsFactionIndex = 0;

        for (int i = 0; i < MedievalFactions.getInstance().factions.size(); i++) {
            if (MedievalFactions.getInstance().factions.get(i).isMember(player1.getUniqueId())) {
                attackersFactionIndex = i;
            }
            if (MedievalFactions.getInstance().factions.get(i).isMember(player2.getUniqueId())) {
                victimsFactionIndex = i;
            }
        }

        return new Pair<>(attackersFactionIndex, victimsFactionIndex);
    }

    // Placed lower as it goes with the method below it.
    private static List<PotionEffectType> BAD_POTION_EFFECTS = Arrays.asList(
            PotionEffectType.BLINDNESS,
            PotionEffectType.CONFUSION,
            PotionEffectType.HARM,
            PotionEffectType.HUNGER,
            PotionEffectType.POISON,
            PotionEffectType.SLOW,
            PotionEffectType.SLOW_DIGGING,
            PotionEffectType.UNLUCK,
            PotionEffectType.WEAKNESS,
            PotionEffectType.WITHER
    );

    public boolean potionEffectBad(PotionEffectType effect) {
        return BAD_POTION_EFFECTS.contains(effect);
    }

    private static List<PotionType> BAD_POTION_TYPES = Arrays.asList(
            PotionType.INSTANT_DAMAGE,
            PotionType.POISON,
            PotionType.SLOWNESS,
            PotionType.WEAKNESS,
            PotionType.TURTLE_MASTER
    );

    public boolean potionTypeBad(PotionType type){
        return BAD_POTION_TYPES.contains(type);
    }

    public static String findPlayerNameBasedOnUUID(UUID playerUUID) {
        // Check online
        for (Player player : getOnlinePlayers()){
            if (player.getUniqueId().equals(playerUUID)){
                return player.getName();
            }
        }

        // Check offline
        for (OfflinePlayer player : getOfflinePlayers()){
            if (player.getUniqueId().equals(playerUUID)){
                return player.getName();
            }
        }

        return "";
    }

    public static UUID findUUIDBasedOnPlayerName(String playerName){
        // Check online
        for (Player player : getOnlinePlayers()){
            if (player.getName().equals(playerName)){
                return player.getUniqueId();
            }
        }

        // Check offline
        for (OfflinePlayer player : getOfflinePlayers()){
            try {
                if (player.getName().equals(playerName)){
                    return player.getUniqueId();
                }
            } catch (NullPointerException e) {
                // Fail silently as quit possibly common.
            }

        }

        return null;
    }

    public void sendAllPlayersOnServerMessage(String message) {
        try {
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendMessage(message);
            }
        }
        catch(Exception ignored) {

        }

    }

    // this method is to ensure that when updating to a version with power decay, even players who
    // never log in again will experience power decay.
    public void createActivityRecordForEveryOfflinePlayer() {
        for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
            PlayerActivityRecord record = UtilitySubsystem.getPlayerActivityRecord(player.getUniqueId(), MedievalFactions.getInstance().playerActivityRecords);
            if (record == null) {
                PlayerActivityRecord newRecord = new PlayerActivityRecord(player.getUniqueId(), 1);
                newRecord.setLastLogout(ZonedDateTime.now());
                MedievalFactions.getInstance().playerActivityRecords.add(newRecord);
            }
        }
    }

    public boolean containsIgnoreCase(ArrayList<String> list, String str) {
        for (String string : list) {
            if (string.equalsIgnoreCase(str)) {
                return true;
            }
        }
        return false;
    }

    public void removeIfContainsIgnoreCase(ArrayList<String> list, String str) {
        String toRemove = "";
        for (String string : list) {
            if (string.equalsIgnoreCase(str)) {
                toRemove = string;
                break;
            }
        }
        list.remove(toRemove);
    }

    private Chunk getChunkByDirection(Chunk origin, String direction) {

        int xpos = -1;
        int zpos = -1;

        if (direction.equalsIgnoreCase("north")) {
            xpos = origin.getX();
            zpos = origin.getZ() + 1;
        }
        if (direction.equalsIgnoreCase("east")) {
            xpos = origin.getX() + 1;
            zpos = origin.getZ();
        }
        if (direction.equalsIgnoreCase("south")) {
            xpos = origin.getX();
            zpos = origin.getZ() - 1;
        }
        if (direction.equalsIgnoreCase("west")) {
            xpos = origin.getX() - 1;
            zpos = origin.getZ();
        }

        return origin.getWorld().getChunkAt(xpos, zpos);
    }

    private ClaimedChunk getClaimedChunk(Chunk chunk) {
        return getClaimedChunk(chunk.getX(), chunk.getZ(), chunk.getWorld().getName(), MedievalFactions.getInstance().claimedChunks);
    }

    // this will return true if the chunks to the North, East, South and West of the target are claimed by the same faction as the target
    private boolean isClaimedChunkSurroundedByChunksClaimedBySameFaction(ClaimedChunk target) {
        ClaimedChunk northernClaimedChunk = getClaimedChunk(getChunkByDirection(target.getChunk(), "north"));
        ClaimedChunk easternClaimedChunk = getClaimedChunk(getChunkByDirection(target.getChunk(), "east"));
        ClaimedChunk southernClaimedChunk = getClaimedChunk(getChunkByDirection(target.getChunk(), "south"));
        ClaimedChunk westernClaimedChunk = getClaimedChunk(getChunkByDirection(target.getChunk(), "west"));

        if (northernClaimedChunk == null ||
            easternClaimedChunk == null ||
            southernClaimedChunk == null ||
            westernClaimedChunk == null) {

            return false;

        }

        boolean northernChunkClaimedBySameFaction = target.getHolder().equalsIgnoreCase(northernClaimedChunk.getHolder());
        boolean easternChunkClaimedBySameFaction = target.getHolder().equalsIgnoreCase(easternClaimedChunk.getHolder());
        boolean southernChunkClaimedBySameFaction = target.getHolder().equalsIgnoreCase(southernClaimedChunk.getHolder());
        boolean westernChunkClaimedBySameFaction = target.getHolder().equalsIgnoreCase(westernClaimedChunk.getHolder());

        return (northernChunkClaimedBySameFaction &&
                easternChunkClaimedBySameFaction &&
                southernChunkClaimedBySameFaction &&
                westernChunkClaimedBySameFaction);
    }

}
