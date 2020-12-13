package dansplugins.factionsystem.utils;

import dansplugins.factionsystem.ConfigManager;
import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.Scheduler;
import dansplugins.factionsystem.data.EphemeralData;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.*;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import java.io.File;
import java.time.ZonedDateTime;
import java.util.*;

import static org.bukkit.Bukkit.*;

public class Utilities {

    private static Utilities instance;

    private Utilities() {

    }

    public static Utilities getInstance() {
        if (instance == null) {
            instance = new Utilities();
        }
        return instance;
    }
    
    public boolean isChest(Block block) {
        return block.getType() == Material.CHEST;
    }

    public PlayerActivityRecord getPlayerActivityRecord(UUID uuid, ArrayList<PlayerActivityRecord> playerActivityRecords)
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
        for (PlayerPowerRecord record : PersistentData.getInstance().getPlayerPowerRecords()) {
            record.setPowerLevel(MedievalFactions.getInstance().getConfig().getInt("initialPowerLevel"));
        }
    }

    public boolean isBlockLocked(Block block) {
        return isBlockLocked(block.getX(), block.getY(), block.getZ(), block.getWorld().getName());
    }

    public boolean isBlockLocked(int x, int y, int z, String world) {
        for (LockedBlock block : PersistentData.getInstance().getLockedBlocks()) {
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
        for (LockedBlock block : PersistentData.getInstance().getLockedBlocks()) {
            if (block.getX() == x && block.getY() == y && block.getZ() == z && block.getWorld().equalsIgnoreCase(world)) {
                return block;
            }
        }
        return null;
    }
    
    public boolean isGateBlock(Block targetBlock)
    {
    	for (Faction faction : PersistentData.getInstance().getFactions())
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

    //  methods ----------------------------

    public Duel getDuel(Player player, Player target)
    {
    	for (Duel duel : EphemeralData.getInstance().getDuelingPlayers())
    	{
    		if (duel.hasPlayer(player) && duel.hasPlayer(target))
    		{
    			return duel;
    		}
    	}
    	return null;
    }

    public Duel getDuel(Player player)
    {
    	for (Duel duel : EphemeralData.getInstance().getDuelingPlayers())
    	{
    		if (duel.isChallenged(player) || duel.isChallenger(player))
    		{
    			return duel;
    		}
    	}
    	return null;
    }
    
    public Faction getGateFaction(Gate gate, ArrayList<Faction> factions)
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
    
    public Gate getGate(Block targetBlock, ArrayList<Faction> factions)
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
    
    public void startCreatingGate(Player player, String name)
    {
    	if (!EphemeralData.getInstance().getCreatingGatePlayers().containsKey(player.getUniqueId()))
    	{
    		Gate gate = new Gate();
    		gate.setName(name);
    		EphemeralData.getInstance().getCreatingGatePlayers().put(player.getUniqueId(), gate);
    	}
    	else
    	{
    		System.out.println("WARNING: Player has already started creating the gate. startCreatingGate() call ignored.");
    	}
    }
    
    public boolean isGateBlock(Block targetBlock, ArrayList<Faction> factions)
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

    public LockedBlock getLockedBlock(Block block, ArrayList<LockedBlock> lockedBlocks) {
        return getLockedBlock(block.getX(), block.getY(), block.getZ(), block.getWorld().getName(), lockedBlocks);
    }

    public  LockedBlock getLockedBlock(int x, int y, int z, String world, ArrayList<LockedBlock> lockedBlocks) {
        for (LockedBlock block : lockedBlocks) {
            if (block.getX() == x && block.getY() == y && block.getZ() == z && block.getWorld().equalsIgnoreCase(world)) {
                return block;
            }
        }
        return null;
    }    

    public  boolean isBlockLocked(Block block, ArrayList<LockedBlock> lockedBlocks) {
        return isBlockLocked(block.getX(), block.getY(), block.getZ(), block.getWorld().getName(), lockedBlocks);
    }

    public  boolean isBlockLocked(int x, int y, int z, String world, ArrayList<LockedBlock> lockedBlocks) {
        for (LockedBlock block : lockedBlocks) {
            if (block.getX() == x && block.getY() == y && block.getZ() == z && block.getWorld().equalsIgnoreCase(world)) {
                return true;
            }
        }
        return false;
    }
   
    public  boolean isInFaction(UUID playerUUID, ArrayList<Faction> factions) {
        // membership check
        for (Faction faction : factions) {
            if (faction.isMember(playerUUID)) {
                return true;
            }
        }
        return false;
    }

    public  Faction getPlayersFaction(UUID playerUUID, ArrayList<Faction> factions) {
        // membership check
        for (Faction faction : factions) {
            if (faction.isMember(playerUUID)) {
                return faction;
            }
        }
        return null;
    }

    public void sendFactionInfo(Player player, Faction faction, int power) {
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

    public void sendFactionMembers(Player player, Faction faction) {
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

    public String createStringFromFirstArgOnwards(String[] args) {
        StringBuilder name = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            name.append(args[i]);
            if (!(i == args.length - 1)) {
                name.append(" ");
            }
        }
        return name.toString();
    }

    public void sendAllPlayersInFactionMessage(Faction faction, String message) {
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

    public PlayerPowerRecord getPlayersPowerRecord(UUID playerUUID, ArrayList<PlayerPowerRecord> powerRecords ) {
        for (PlayerPowerRecord record : powerRecords) {
            if (record.getPlayerUUID().equals(playerUUID)) {
                return record;
            }
        }
        return null;
    }

    public Faction getFaction(String name, ArrayList<Faction> factions) {
        for (Faction faction : factions) {
            if (faction.getName().equalsIgnoreCase(name)) {
                return faction;
            }
        }
        return null;
    }

    public boolean isPlayerAFactionOwner(UUID player, ArrayList<Faction> factions){
        if (isInFaction(player, factions)){
            Faction faction = getPlayersFaction(player, factions);
            return faction.getOwner().equals(player);
        } else {
            return false;
        }
    }

    public boolean isPlayerAFactionOfficer(UUID player, ArrayList<Faction> factions) {
        if (isInFaction(player, factions)){
            Faction faction = getPlayersFaction(player, factions);
            return faction.isOfficer(player);
        } else {
            return false;
        }
    }

    public void invokeAlliances(String victimFactionName, String declaringFactionName, ArrayList<Faction> factions) {
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

    public void removeAllLocks(String factionName, ArrayList<LockedBlock> lockedBlocks) {
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
            ConfigManager.getInstance().handleVersionMismatch();
        }
    }

    public boolean arePlayersFactionsNotEnemies(Player player1, Player player2) {
        Pair<Integer, Integer> factionIndices = getFactionIndices(player1, player2);
        int attackersFactionIndex = factionIndices.getLeft();
        int victimsFactionIndex = factionIndices.getRight();

        return !(PersistentData.getInstance().getFactions().get(attackersFactionIndex).isEnemy(PersistentData.getInstance().getFactions().get(victimsFactionIndex).getName())) &&
                !(PersistentData.getInstance().getFactions().get(victimsFactionIndex).isEnemy(PersistentData.getInstance().getFactions().get(attackersFactionIndex).getName()));
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
        return isInFaction(player1.getUniqueId(), PersistentData.getInstance().getFactions()) && isInFaction(player2.getUniqueId(), PersistentData.getInstance().getFactions());
    }

    public Pair<Integer, Integer> getFactionIndices(Player player1, Player player2){
        int attackersFactionIndex = 0;
        int victimsFactionIndex = 0;

        for (int i = 0; i < PersistentData.getInstance().getFactions().size(); i++) {
            if (PersistentData.getInstance().getFactions().get(i).isMember(player1.getUniqueId())) {
                attackersFactionIndex = i;
            }
            if (PersistentData.getInstance().getFactions().get(i).isMember(player2.getUniqueId())) {
                victimsFactionIndex = i;
            }
        }

        return new Pair<>(attackersFactionIndex, victimsFactionIndex);
    }

    // Placed lower as it goes with the method below it.
    private  List<PotionEffectType> BAD_POTION_EFFECTS = Arrays.asList(
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

    private List<PotionType> BAD_POTION_TYPES = Arrays.asList(
            PotionType.INSTANT_DAMAGE,
            PotionType.POISON,
            PotionType.SLOWNESS,
            PotionType.WEAKNESS,
            PotionType.TURTLE_MASTER
    );

    public boolean potionTypeBad(PotionType type){
        return BAD_POTION_TYPES.contains(type);
    }

    public String findPlayerNameBasedOnUUID(UUID playerUUID) {
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

    public  UUID findUUIDBasedOnPlayerName(String playerName){
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
            PlayerActivityRecord record = getPlayerActivityRecord(player.getUniqueId(), PersistentData.getInstance().getPlayerActivityRecords());
            if (record == null) {
                PlayerActivityRecord newRecord = new PlayerActivityRecord(player.getUniqueId(), 1);
                newRecord.setLastLogout(ZonedDateTime.now());
                PersistentData.getInstance().getPlayerActivityRecords().add(newRecord);
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

}
