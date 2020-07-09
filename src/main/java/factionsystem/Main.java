package factionsystem;

import factionsystem.EventHandlers.*;
import factionsystem.Objects.ClaimedChunk;
import factionsystem.Objects.Faction;
import factionsystem.Objects.LockedBlock;
import factionsystem.Objects.PlayerPowerRecord;
import factionsystem.Subsystems.CommandSubsystem;
import factionsystem.Subsystems.StorageSubsystem;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import static factionsystem.Utility.UtilityFunctions.*;

public class Main extends JavaPlugin implements Listener {

    // version
    public static String version = "v2.5";

    // subsystems
    public StorageSubsystem storage = new StorageSubsystem(this);

    // saved lists
    public ArrayList<Faction> factions = new ArrayList<>();
    public ArrayList<ClaimedChunk> claimedChunks = new ArrayList<>();
    public ArrayList<PlayerPowerRecord> playerPowerRecords = new ArrayList<>();
    public ArrayList<LockedBlock> lockedBlocks = new ArrayList<>();

    // temporary lists
    public ArrayList<String> lockingPlayers = new ArrayList<>();
    public ArrayList<String> unlockingPlayers = new ArrayList<>();
    public HashMap<String, String> playersGrantingAccess = new HashMap<>();
    public ArrayList<String> playersCheckingAccess = new ArrayList<>();
    public HashMap<String, String> playersRevokingAccess = new HashMap<>();

    @Override
    public void onEnable() {
        System.out.println("Medieval Factions plugin enabling....");
        schedulePowerIncrease();
        scheduleAutosave();
        this.getServer().getPluginManager().registerEvents(this, this);
        storage.load();
        System.out.println("Medieval Factions plugin enabled.");
    }

    @Override
    public void onDisable(){
        System.out.println("Medieval Factions plugin disabling....");
        storage.save();
        System.out.println("Medieval Factions plugin disabled.");
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        CommandSubsystem commandInterpreter = new CommandSubsystem(this);
        return commandInterpreter.interpretCommand(sender, label, args);
    }

    // event handlers

    @EventHandler()
    public void onDamage(EntityDamageByEntityEvent event) {
        EntityDamageByEntityEventHandler handler = new EntityDamageByEntityEventHandler(this);
        handler.handle(event);
    }

    @EventHandler()
    public void onPlayerMove(PlayerMoveEvent event) {
        PlayerMoveEventHandler handler = new PlayerMoveEventHandler(this);
        handler.handle(event);
    }

    @EventHandler()
    public void onBlockBreak(BlockBreakEvent event) {
        BlockBreakEventHandler handler = new BlockBreakEventHandler(this);
        handler.handle(event);
    }
    @EventHandler()
    public void onBlockPlace(BlockPlaceEvent event) {
        BlockPlaceEventHandler handler = new BlockPlaceEventHandler(this);
        handler.handle(event);
    }

    @EventHandler()
    public void onRightClick(PlayerInteractEvent event) {
        PlayerInteractEventHandler handler = new PlayerInteractEventHandler(this);
        handler.handle(event);
    }

    @EventHandler()
    public void onJoin(PlayerJoinEvent event) {
        if (!hasPowerRecord(event.getPlayer().getName())) {
            PlayerPowerRecord newRecord = new PlayerPowerRecord(event.getPlayer().getName());
            playerPowerRecords.add(newRecord);
        }
    }

    @EventHandler()
    public void onDeath(PlayerDeathEvent event) {
        PlayerDeathEventHandler handler = new PlayerDeathEventHandler(this);
        handler.handle(event);
    }

    // main utility methods ----------------------------------------------------------------------------------------------------------------
    // TODO: move these into classes that use them rather than referencing them from main

    public void addChunkAtPlayerLocation(Player player) {
        double[] playerCoords = new double[2];
        playerCoords[0] = player.getLocation().getChunk().getX();
        playerCoords[1] = player.getLocation().getChunk().getZ();
        for (Faction faction : factions) {
            if (faction.isOwner(player.getName()) || faction.isOfficer(player.getName())) {

                // check if land is already claimed
                for (ClaimedChunk chunk : claimedChunks) {
                    if (playerCoords[0] == chunk.getCoordinates()[0] && playerCoords[1] == chunk.getCoordinates()[1]) {

                        // if holder is player's faction
                        if (chunk.getHolder().equalsIgnoreCase(faction.getName()) && getPlayersFaction(player.getName(), factions).getAutoClaimStatus() == false) {
                            player.sendMessage(ChatColor.RED + "This land is already claimed by your faction!");
                            return;
                        }
                        else {

                            // check if faction has more land than their demesne limit
                            for (Faction targetFaction : factions) {
                                if (chunk.getHolder().equalsIgnoreCase(targetFaction.getName())) {
                                    if (targetFaction.getCumulativePowerLevel() < getChunksClaimedByFaction(targetFaction.getName(), claimedChunks)) {

                                        // is at war with target faction
                                        if (faction.isEnemy(targetFaction.getName())) {

                                            // remove locks on this chunk
                                            Iterator<LockedBlock> itr = lockedBlocks.iterator();
                                            while (itr.hasNext()) {
                                                LockedBlock block = itr.next();
                                                if (chunk.getChunk().getWorld().getBlockAt(block.getX(), block.getY(), block.getZ()).getChunk().getX() == chunk.getChunk().getX() &&
                                                        chunk.getChunk().getWorld().getBlockAt(block.getX(), block.getY(), block.getZ()).getChunk().getZ() == chunk.getChunk().getZ()) {
                                                    itr.remove();
                                                }
                                            }

                                            claimedChunks.remove(chunk);

                                            ClaimedChunk newChunk = new ClaimedChunk(player.getLocation().getChunk());
                                            newChunk.setHolder(faction.getName());
                                            newChunk.setWorld(player.getLocation().getWorld().getName());
                                            claimedChunks.add(newChunk);
                                            player.sendMessage(ChatColor.GREEN + "Land conquered from " + targetFaction.getName() + "! Demesne Size: " + getChunksClaimedByFaction(faction.getName(), claimedChunks) + "/" + faction.getCumulativePowerLevel());

                                            sendAllPlayersInFactionMessage(targetFaction, ChatColor.RED + getPlayersFaction(player.getName(), factions).getName() + " has conquered land from your faction!");

                                            return;
                                        }
                                        else {
                                            player.sendMessage(ChatColor.RED + "Your factions have to be at war in order for you to conquer land.");
                                            return;
                                        }
                                    }
                                }
                            }

                            if (!getPlayersFaction(player.getName(), factions).getAutoClaimStatus()) {
                                player.sendMessage(ChatColor.RED + "This land is already claimed by " + chunk.getHolder());
                            }

                            return;
                        }
                    }
                }

                ClaimedChunk newChunk = new ClaimedChunk(player.getLocation().getChunk());
                newChunk.setHolder(faction.getName());
                newChunk.setWorld(player.getLocation().getWorld().getName());
                claimedChunks.add(newChunk);
                player.sendMessage(ChatColor.GREEN + "Land claimed! Demesne Size: " + getChunksClaimedByFaction(faction.getName(), claimedChunks) + "/" + faction.getCumulativePowerLevel());
                return;
            }
        }
    }

    public void removeChunkAtPlayerLocation(Player player) {
        double[] playerCoords = new double[2];
        playerCoords[0] = player.getLocation().getChunk().getX();
        playerCoords[1] = player.getLocation().getChunk().getZ();
        for (Faction faction : factions) {
            if (faction.isOwner(player.getName()) || faction.isOfficer(player.getName())) {

                // check if land is claimed by player's faction
                for (ClaimedChunk chunk : claimedChunks) {
                    if (playerCoords[0] == chunk.getCoordinates()[0] && playerCoords[1] == chunk.getCoordinates()[1]) {
                        // if holder is player's faction
                        if (chunk.getHolder().equalsIgnoreCase(faction.getName())) {

                            String identifier = (int)chunk.getChunk().getX() + "_" + (int)chunk.getChunk().getZ();

                            // delete file associated with chunk
                            System.out.println("Attempting to delete file plugins plugins/medievalfactions/claimedchunks/" + identifier + ".txt");
                            try {
                                File fileToDelete = new File("plugins/medievalfactions/claimedchunks/" + identifier + ".txt");
                                if (fileToDelete.delete()) {
                                    System.out.println("Success. File deleted.");
                                }
                                else {
                                    System.out.println("There was a problem deleting the file.");
                                }
                            } catch(Exception e) {
                                System.out.println("There was a problem encountered during file deletion.");
                            }

                            // if faction home is located on this chunk
                            Location factionHome = getPlayersFaction(player.getName(), factions).getFactionHome();
                            if (factionHome != null) {
                                if (factionHome.getChunk().getX() == chunk.getChunk().getX() && factionHome.getChunk().getZ() == chunk.getChunk().getZ()) {

                                    // remove faction home
                                    faction.setFactionHome(null);
                                    sendAllPlayersInFactionMessage(faction, ChatColor.RED + "Your faction home has been removed!");

                                }
                            }

                            // remove locks on this chunk
                            Iterator<LockedBlock> itr = lockedBlocks.iterator();
                            while (itr.hasNext()) {
                                LockedBlock block = itr.next();
                                if (chunk.getChunk().getWorld().getBlockAt(block.getX(), block.getY(), block.getZ()).getChunk().getX() == chunk.getChunk().getX() &&
                                        chunk.getChunk().getWorld().getBlockAt(block.getX(), block.getY(), block.getZ()).getChunk().getZ() == chunk.getChunk().getZ()) {
                                    itr.remove();
                                }
                            }

                            claimedChunks.remove(chunk);
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
    }

    public String checkOwnershipAtPlayerLocation(Player player) {
        double[] playerCoords = new double[2];
        playerCoords[0] = player.getLocation().getChunk().getX();
        playerCoords[1] = player.getLocation().getChunk().getZ();
        System.out.println("Checking if chunk at location of player " + player.getName() + " is claimed.");
        for (ClaimedChunk chunk : claimedChunks) {
//            System.out.println("Comparing player coords " + playerCoords[0] + ", " + playerCoords[1] + " to chunk coords " + chunk.getCoordinates()[0] + ", " + chunk.getCoordinates()[1]);
            if (playerCoords[0] == chunk.getCoordinates()[0] && playerCoords[1] == chunk.getCoordinates()[1]) {
                System.out.println("Match!");
                return chunk.getHolder();
            }
        }
        System.out.println("No match found.");
        return "unclaimed";
    }

    public void removeLock(Block block) {
        for (LockedBlock b : lockedBlocks) {
            if (b.getX() == block.getX() && b.getY() == block.getY() && b.getZ() == block.getZ()) {
                lockedBlocks.remove(b);
                return;
            }
        }
    }

    public boolean isDoor(Block block) {
        if (block.getType() == Material.ACACIA_DOOR ||
                block.getType() == Material.BIRCH_DOOR ||
                block.getType() == Material.DARK_OAK_DOOR ||
                block.getType() == Material.IRON_DOOR ||
                block.getType() == Material.JUNGLE_DOOR ||
                block.getType() == Material.OAK_DOOR ||
                block.getType() == Material.SPRUCE_DOOR) {

            return true;

        }
        return false;
    }

    public boolean isChest(Block block) {
        if (block.getType() == Material.CHEST) {
            return true;
        }
        return false;
    }

    public boolean hasPowerRecord(String playerName) {
        for (PlayerPowerRecord record : playerPowerRecords) {
            if (record.getPlayerName().equalsIgnoreCase(playerName)) {
                return true;
            }
        }
        return false;
    }

    public void schedulePowerIncrease() {
        System.out.println("Scheduling hourly power increase...");
        int delay = 30 * 60; // 30 minutes
        int secondsUntilRepeat = 60 * 60; // 1 hour
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
            @Override
            public void run() {
                System.out.println("Medieval Factions is increasing the power of every player by 1 if their power is below 10. This will happen hourly.");
                for (PlayerPowerRecord powerRecord : playerPowerRecords) {
                    try {
                        if (powerRecord.getPowerLevel() < 20) {
                            if (Bukkit.getServer().getPlayer(powerRecord.getPlayerName()).isOnline()) {
                                powerRecord.increasePower();
                                if (isInFaction(powerRecord.getPlayerName(), factions)) {
                                    getPlayersFaction(powerRecord.getPlayerName(), factions).addPower();
                                }
                                Bukkit.getServer().getPlayer(powerRecord.getPlayerName()).sendMessage(ChatColor.GREEN + "You feel stronger. Your power has increased.");
                            }
                        }
                    } catch (Exception ignored) {
                        // player offline
                    }
                }
            }
        }, delay * 20, secondsUntilRepeat * 20);
    }

    public void scheduleAutosave() {
        System.out.println("Scheduling hourly auto save...");
        int delay = 60 * 60; // 1 hour
        int secondsUntilRepeat = 60 * 60; // 1 hour
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
            @Override
            public void run() {
                System.out.println("Medieval Factions is saving. This will happen every hour.");
                storage.save();
            }
        }, delay * 20, secondsUntilRepeat * 20);
    }

    public void resetPowerRecords() {
        // reset individual records
        System.out.println("Resetting individual power records.");
        for (PlayerPowerRecord record : playerPowerRecords) {
            record.setPowerLevel(10);
        }

        // reset faction cumulative power levels
        System.out.println("Resetting faction cumulative power records.");
        for (Faction faction : factions) {
            int sum = 0;
            for (String playerName : faction.getMemberArrayList()) {
                sum = sum + getPlayersPowerRecord(playerName, playerPowerRecords).getPowerLevel();
            }
            faction.setCumulativePowerLevel(sum);
        }

    }

    public boolean isBlockLocked(Block block) {
        return isBlockLocked(block.getX(), block.getY(), block.getZ());
    }

    public boolean isBlockLocked(int x, int y, int z) {
        for (LockedBlock block : lockedBlocks) {
            if (block.getX() == x && block.getY() == y && block.getZ() == z) {
                return true;
            }
        }
        return false;
    }

    public LockedBlock getLockedBlock(Block block) {
        return getLockedBlock(block.getX(), block.getY(), block.getZ());
    }

    public LockedBlock getLockedBlock(int x, int y, int z) {
        for (LockedBlock block : lockedBlocks) {
            if (block.getX() == x && block.getY() == y && block.getZ() == z) {
                return block;
            }
        }
        return null;
    }
}