package dansplugins.factionsystem;

import dansplugins.factionsystem.objects.Faction;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.UUID;

import static org.bukkit.Bukkit.getServer;

public class Messenger {

    private static Messenger instance;

    private Messenger() {

    }

    public static Messenger getInstance() {
        if (instance == null) {
            instance = new Messenger();
        }
        return instance;
    }

    public void sendFactionInfo(Player player, Faction faction, int power) {
        player.sendMessage(ChatColor.BOLD + "" + ChatColor.AQUA + faction.getName() + " Faction Info" + "\n----------\n");
        player.sendMessage(ChatColor.AQUA + "Name: " + faction.getName() + "\n");
        player.sendMessage(ChatColor.AQUA + "Owner: " + UUIDChecker.getInstance().findPlayerNameBasedOnUUID(faction.getOwner()) + "\n");
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

    public void sendAllPlayersOnServerMessage(String message) {
        try {
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendMessage(message);
            }
        }
        catch(Exception ignored) {

        }

    }

}
