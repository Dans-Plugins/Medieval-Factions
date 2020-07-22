package factionsystem.Subsystems;

import factionsystem.Main;
import factionsystem.Objects.ClaimedChunk;
import factionsystem.Objects.Faction;
import factionsystem.Objects.LockedBlock;
import factionsystem.Objects.PlayerPowerRecord;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

public class UtilitySubsystem {

    Main main = null;

    public UtilitySubsystem(Main plugin) {
        main = plugin;
    }

    // non-static methods

    public void addChunkAtPlayerLocation(Player player) {
        double[] playerCoords = new double[2];
        playerCoords[0] = player.getLocation().getChunk().getX();
        playerCoords[1] = player.getLocation().getChunk().getZ();
        for (Faction faction : main.factions) {
            if (faction.isOwner(player.getName()) || faction.isOfficer(player.getName())) {

                // check if land is already claimed
                for (ClaimedChunk chunk : main.claimedChunks) {
                    if (playerCoords[0] == chunk.getCoordinates()[0] && playerCoords[1] == chunk.getCoordinates()[1]) {

                        // if holder is player's faction
                        if (chunk.getHolder().equalsIgnoreCase(faction.getName()) && getPlayersFaction(player.getName(), main.factions).getAutoClaimStatus() == false) {
                            player.sendMessage(ChatColor.RED + "This land is already claimed by your faction!");
                            return;
                        }
                        else {

                            // check if faction has more land than their demesne limit
                            for (Faction targetFaction : main.factions) {
                                if (chunk.getHolder().equalsIgnoreCase(targetFaction.getName())) {
                                    if (targetFaction.getCumulativePowerLevel() < getChunksClaimedByFaction(targetFaction.getName(), main.claimedChunks)) {

                                        // is at war with target faction
                                        if (faction.isEnemy(targetFaction.getName())) {

                                            // remove locks on this chunk
                                            Iterator<LockedBlock> itr = main.lockedBlocks.iterator();
                                            while (itr.hasNext()) {
                                                LockedBlock block = itr.next();
                                                if (chunk.getChunk().getWorld().getBlockAt(block.getX(), block.getY(), block.getZ()).getChunk().getX() == chunk.getChunk().getX() &&
                                                        chunk.getChunk().getWorld().getBlockAt(block.getX(), block.getY(), block.getZ()).getChunk().getZ() == chunk.getChunk().getZ()) {
                                                    itr.remove();
                                                }
                                            }

                                            main.claimedChunks.remove(chunk);

                                            ClaimedChunk newChunk = new ClaimedChunk(player.getLocation().getChunk());
                                            newChunk.setHolder(faction.getName());
                                            newChunk.setWorld(player.getLocation().getWorld().getName());
                                            main.claimedChunks.add(newChunk);
                                            player.sendMessage(ChatColor.GREEN + "Land conquered from " + targetFaction.getName() + "! Demesne Size: " + getChunksClaimedByFaction(faction.getName(), main.claimedChunks) + "/" + faction.getCumulativePowerLevel());

                                            sendAllPlayersInFactionMessage(targetFaction, ChatColor.RED + getPlayersFaction(player.getName(), main.factions).getName() + " has conquered land from your faction!");

                                            return;
                                        }
                                        else {
                                            player.sendMessage(ChatColor.RED + "Your factions have to be at war in order for you to conquer land.");
                                            return;
                                        }
                                    }
                                }
                            }

                            if (!getPlayersFaction(player.getName(), main.factions).getAutoClaimStatus()) {
                                player.sendMessage(ChatColor.RED + "This land is already claimed by " + chunk.getHolder());
                            }

                            return;
                        }
                    }
                }

                ClaimedChunk newChunk = new ClaimedChunk(player.getLocation().getChunk());
                newChunk.setHolder(faction.getName());
                newChunk.setWorld(player.getLocation().getWorld().getName());
                main.claimedChunks.add(newChunk);
                player.sendMessage(ChatColor.GREEN + "Land claimed! Demesne Size: " + getChunksClaimedByFaction(faction.getName(), main.claimedChunks) + "/" + faction.getCumulativePowerLevel());
                return;
            }
        }
    }

    public void removeChunkAtPlayerLocation(Player player) {
        double[] playerCoords = new double[2];
        playerCoords[0] = player.getLocation().getChunk().getX();
        playerCoords[1] = player.getLocation().getChunk().getZ();
        for (Faction faction : main.factions) {
            if (faction.isOwner(player.getName()) || faction.isOfficer(player.getName())) {

                // check if land is claimed by player's faction
                for (ClaimedChunk chunk : main.claimedChunks) {
                    if (playerCoords[0] == chunk.getCoordinates()[0] && playerCoords[1] == chunk.getCoordinates()[1]) {
                        // if holder is player's faction
                        if (chunk.getHolder().equalsIgnoreCase(faction.getName())) {

                            String identifier = (int)chunk.getChunk().getX() + "_" + (int)chunk.getChunk().getZ();

                            // delete file associated with chunk
                            System.out.println("Attempting to delete file plugins plugins/MedievalFactions/claimedchunks/" + identifier + ".txt");
                            try {
                                File fileToDelete = new File("plugins/MedievalFactions/claimedchunks/" + identifier + ".txt");
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
                            Location factionHome = getPlayersFaction(player.getName(), main.factions).getFactionHome();
                            if (factionHome != null) {
                                if (factionHome.getChunk().getX() == chunk.getChunk().getX() && factionHome.getChunk().getZ() == chunk.getChunk().getZ()) {

                                    // remove faction home
                                    faction.setFactionHome(null);
                                    sendAllPlayersInFactionMessage(faction, ChatColor.RED + "Your faction home has been removed!");

                                }
                            }

                            // remove locks on this chunk
                            Iterator<LockedBlock> itr = main.lockedBlocks.iterator();
                            while (itr.hasNext()) {
                                LockedBlock block = itr.next();
                                if (chunk.getChunk().getWorld().getBlockAt(block.getX(), block.getY(), block.getZ()).getChunk().getX() == chunk.getChunk().getX() &&
                                        chunk.getChunk().getWorld().getBlockAt(block.getX(), block.getY(), block.getZ()).getChunk().getZ() == chunk.getChunk().getZ()) {
                                    itr.remove();
                                }
                            }

                            main.claimedChunks.remove(chunk);
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
        for (ClaimedChunk chunk : main.claimedChunks) {
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
        for (LockedBlock b : main.lockedBlocks) {
            if (b.getX() == block.getX() && b.getY() == block.getY() && b.getZ() == block.getZ()) {
                main.lockedBlocks.remove(b);
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
        for (PlayerPowerRecord record : main.playerPowerRecords) {
            if (record.getPlayerName().equalsIgnoreCase(playerName)) {
                return true;
            }
        }
        return false;
    }

    public void schedulePowerIncrease() {

        int maxPower = 50;

        System.out.println("Scheduling hourly power increase...");
        int delay = 30 * 60; // 30 minutes
        int secondsUntilRepeat = 60 * 60; // 1 hour
        Bukkit.getScheduler().scheduleSyncRepeatingTask(main, new Runnable() {
            @Override
            public void run() {
                System.out.println("Medieval Factions is increasing the power of every player by " + main.getConfig().getInt("hourlyPowerIncreaseAmount") + " if their power is below 10. This will happen hourly.");
                for (PlayerPowerRecord powerRecord : main.playerPowerRecords) {
                    try {
                        if (powerRecord.getPowerLevel() < maxPower) {
                            if (Bukkit.getServer().getPlayer(powerRecord.getPlayerName()).isOnline()) {
                                powerRecord.increasePower();
                                if (isInFaction(powerRecord.getPlayerName(), main.factions)) {
                                    getPlayersFaction(powerRecord.getPlayerName(), main.factions).addPower(main.getConfig().getInt("hourlyPowerIncreaseAmount"));
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
        Bukkit.getScheduler().scheduleSyncRepeatingTask(main, new Runnable() {
            @Override
            public void run() {
                System.out.println("Medieval Factions is saving. This will happen every hour.");
                main.storage.save();
            }
        }, delay * 20, secondsUntilRepeat * 20);
    }

    public void resetPowerRecords() {
        // reset individual records
        System.out.println("Resetting individual power records.");
        for (PlayerPowerRecord record : main.playerPowerRecords) {
            record.setPowerLevel(10);
        }

        // reset faction cumulative power levels
        System.out.println("Resetting faction cumulative power records.");
        for (Faction faction : main.factions) {
            int sum = 0;
            for (String playerName : faction.getMemberArrayList()) {
                sum = sum + getPlayersPowerRecord(playerName, main.playerPowerRecords).getPowerLevel();
            }
            faction.setCumulativePowerLevel(sum);
        }

    }

    public boolean isBlockLocked(Block block) {
        return isBlockLocked(block.getX(), block.getY(), block.getZ());
    }

    public boolean isBlockLocked(int x, int y, int z) {
        for (LockedBlock block : main.lockedBlocks) {
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
        for (LockedBlock block : main.lockedBlocks) {
            if (block.getX() == x && block.getY() == y && block.getZ() == z) {
                return block;
            }
        }
        return null;
    }

    public ArrayList<Faction> getFactionsSortedByPower() {
        ArrayList<Faction> copiedList = new ArrayList<>(main.factions);
        ArrayList<Faction> sortedList = new ArrayList<>();
        while (copiedList.size() != 0) {
            int mostPower = 0;
            int counter = 0;
            int nextMostPowerfulFaction = 0;
            for (Faction faction : copiedList) {
                if (faction.getCumulativePowerLevel() > mostPower) {
                    mostPower = faction.getCumulativePowerLevel();
                    nextMostPowerfulFaction = counter;
                }
                counter++;
            }
            sortedList.add(copiedList.get(nextMostPowerfulFaction));
            copiedList.remove(nextMostPowerfulFaction);
        }
        return sortedList;
    }

    // static methods ----------------------------

    public static boolean isInFaction(String playerName, ArrayList<Faction> factions) {
        // membership check
        boolean isAlreadyInFaction = false;
        for (Faction faction : factions) {
            if (faction.isMember(playerName)) {
                isAlreadyInFaction = true;
                break;
            }
        }
        return isAlreadyInFaction;
    }

    public static Faction getPlayersFaction(String playerName, ArrayList<Faction> factions) {
        // membership check
        boolean isAlreadyInFaction = false;
        for (Faction faction : factions) {
            if (faction.isMember(playerName)) {
                return faction;
            }
        }
        return null;
    }

    public static void sendFactionInfo(Player player, Faction faction, int power) {
        player.sendMessage(ChatColor.BOLD + "" + ChatColor.AQUA + faction.getName() + " Faction Info" + "\n----------\n");
        player.sendMessage(ChatColor.AQUA + "Name: " + faction.getName() + "\n");
        player.sendMessage(ChatColor.AQUA + "Owner: " + faction.getOwner() + "\n");
        player.sendMessage(ChatColor.AQUA + "Description: " + faction.getDescription() + "\n");
        player.sendMessage(ChatColor.AQUA + "Population: " + faction.getMemberList().size() + "\n");
        player.sendMessage(ChatColor.AQUA + "Allied With: " + faction.getAlliesSeparatedByCommas() + "\n");
        player.sendMessage(ChatColor.AQUA + "At War With: " + faction.getEnemiesSeparatedByCommas() + "\n");
        player.sendMessage(ChatColor.AQUA + "Power Level: " + faction.getCumulativePowerLevel() + "\n");
        player.sendMessage(ChatColor.AQUA + "Demesne Size: " + power + "/" + faction.getCumulativePowerLevel() + "\n");
        player.sendMessage(ChatColor.AQUA + "----------\n");
    }

    public static void sendFactionMembers(Player player, Faction faction) {
        ArrayList<String> members = faction.getMemberList();
        player.sendMessage(ChatColor.BOLD + "" + ChatColor.AQUA + "Members of " + faction.getName() + "\n----------\n");
        for (String member : members) {
            player.sendMessage(ChatColor.AQUA + member + "\n");
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
        ArrayList<String> members = faction.getMemberArrayList();
        for (String member : members) {
            try {
                Player target = Bukkit.getServer().getPlayer(member);
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

    public static PlayerPowerRecord getPlayersPowerRecord(String playerName, ArrayList<PlayerPowerRecord> powerRecords ) {
        for (PlayerPowerRecord record : powerRecords) {
            if (record.getPlayerName().equalsIgnoreCase(playerName)) {
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
            if (claimedChunk.getCoordinates()[0] == chunk.getX() && claimedChunk.getCoordinates()[1] == chunk.getZ()) {
                return true;
            }
        }
        return false;
    }

    public static ClaimedChunk getClaimedChunk(int x, int z, ArrayList<ClaimedChunk> claimedChunks) {
        for (ClaimedChunk claimedChunk : claimedChunks) {
            if (claimedChunk.getCoordinates()[0] == x && claimedChunk.getCoordinates()[1] == z) {
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
        if (saveFolder.exists()) {
            System.out.println("[ALERT] Old save folder name (pre v3.2) detected. Updating for compatibility.");

            // rename directory
            File newSaveFolder = new File("./plugins/MedievalFactions/");
            saveFolder.renameTo(newSaveFolder);
        }
    }

}
