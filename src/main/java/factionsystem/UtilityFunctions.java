package factionsystem;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

public class UtilityFunctions {

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
        player.sendMessage(ChatColor.AQUA + "At War With: " + faction.getEnemiesSeparatedByCommas() + "\n");
        player.sendMessage(ChatColor.AQUA + "Power Level: " + faction.getCumulativePowerLevel());
        player.sendMessage(ChatColor.AQUA + "Demesne Size: " + power + "/" + faction.getCumulativePowerLevel());
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
                    System.out.println("Attempting to delete file plugins plugins/medievalfactions/claimedchunks/" + identifier + ".txt");
                    File fileToDelete = new File("plugins/medievalfactions/claimedchunks/" + identifier + ".txt");
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
}
